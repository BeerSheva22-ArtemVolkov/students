package telran.spring.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Avg;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;
import telran.spring.exceptions.NotFoundException;
import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.*;
import telran.spring.students.repo.StudentRepository;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
@Service
@Slf4j
public class StudentServiceImpl implements StudentService {

	private static final String AVG_SCORE_FIELD = "avgScore";
	final StudentRepository studentRepository;
	final MongoTemplate mongoTemplate;

	@Value("${app.students.mark.good:80}")
	int goodMark;

	@Override
	@Transactional(readOnly = false) // Синхронизация с бд
	public StudentDto addStudent(StudentDto studentDto) {
		if (studentRepository.existsById(studentDto.id())) {
			throw new IllegalStateException(String.format("student with id %d already exists", studentDto.id()));
		}
		StudentDoc studentDoc = StudentDoc.of(studentDto);
		StudentDto studentRes = studentRepository.save(studentDoc).build();
		log.trace("Student {} has been added", studentRes);
		return studentRes;
	}

	@Override
	@Transactional
	public void addMark(Long studentId, MarkDto markDto) {
		StudentDoc studentDoc = studentRepository.findById(studentId).orElseThrow(
				() -> new NotFoundException(String.format("Student with id %s doesn't exists", studentId)));
		List<MarkDto> marks = studentDoc.getMarks();
		marks.add(markDto);
		studentRepository.save(studentDoc);
	}

	@Override
	public List<MarkDto> getMarksStudentSubject(long studentId, String subject) {
		List<MarkDto> res = Collections.emptyList();
		SubjectMark allMarks = studentRepository.findByIdAndMarksSubjectEquals(studentId, subject);
		if (allMarks != null) {
			res = allMarks.getMarks().stream().filter(m -> m.subject().equals(subject)).toList();
		}
		return res;
	}

	@Override
	public List<MarkDto> getMarksStudentDates(long studentId, LocalDate fromDate, LocalDate toDate) {
		List<MarkDto> res = Collections.emptyList();
		SubjectMark allMarks = studentRepository.findByIdAndMarksDateBetween(studentId, fromDate, toDate);
		if (allMarks != null) {
			res = allMarks.getMarks().stream().filter(m -> {
				LocalDate date = m.date();
				return date.compareTo(fromDate) >= 0 && date.compareTo(toDate) <= 0;
			}).toList();
		}
		return res;
	}

	@Override
	public List<StudentDto> getStudentsPhonePrefix(String phone) {
		return studentRepository.findStudentsPhonePrefix(phone).stream().map(StudentDoc::build).toList();
	}

	@Override
	public List<IdName> getStudentsAllScoresGreater(int score) {
		return studentRepository.findStudentsAllMarksGreater(score);
	}

	@Override
	public List<Long> removeStudentsWithFewMarks(int nMarks) {
		List<StudentDoc> studentsRemoved = studentRepository.removeStudentsFewMarks(nMarks);
		return studentsRemoved.stream().map(StudentDoc::getId).toList();
	}

	@Override
	public List<IdName> getStudentsScoresSubjectGreater(int score, String subject) {
		return studentRepository.findStudentsScoresSubjectGreaterThan(score, subject);
	}

	@Override
	public List<Long> removeStudentsNoLowMarks(int score) {
		List<StudentDoc> studentsRemoved = studentRepository.removeStudentsNoLowMarks(score);
		return studentsRemoved.stream().map(StudentDoc::getId).toList();
	}

	@Override
	public double getStudentsAvgScore() {
		UnwindOperation unwindOperation = unwind("marks");
		GroupOperation groupOperation = group().avg("marks.score").as(AVG_SCORE_FIELD);
		Aggregation pipeline = newAggregation(List.of(unwindOperation, groupOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		double res = aggregationResult.getUniqueMappedResult().getDouble(AVG_SCORE_FIELD);
		return res;
	}

	@Override
	public List<IdName> getGoodStudents() {
		log.debug("good threshold is {}", goodMark);
		return getStudentsAvgMarkGreater(goodMark);
	}

	@Override
	public List<IdName> getStudentsAvgMarkGreater(int score) {
		UnwindOperation unwindOperation = unwind("marks");
		GroupOperation groupOperation = group("id", "name").avg("marks.score").as(AVG_SCORE_FIELD);
		MatchOperation matchOperation = match(Criteria.where(AVG_SCORE_FIELD).gt(score));
		SortOperation sortOperation = sort(Direction.DESC, AVG_SCORE_FIELD);
		ProjectionOperation projectionOperation = project().andExclude(AVG_SCORE_FIELD);
		Aggregation pipeline = newAggregation(
				List.of(unwindOperation, groupOperation, matchOperation, sortOperation, projectionOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> resultDocuments = aggregationResult.getMappedResults();
		return resultDocuments.stream().map(this::toIdName).toList();
	}

	// анонимный класс
	IdName toIdName(Document document) {
		return new IdName() {

			Document idDocument = document.get("_id", Document.class);

			@Override
			public String getName() {
				return idDocument.getString("name");
			}

			@Override
			public long getId() {
				return idDocument.getLong("id");
			}
		};

	}

	@Override
	public List<IdNameMarks> findStudents(String jsonQuery) {
		BasicQuery query = new BasicQuery(jsonQuery);
		List<StudentDoc> students = mongoTemplate.find(query, StudentDoc.class);
		return students.stream().map(this::toIdNameMarks).toList();
	}

	@Override
	public List<IdNameMarks> getBestStudents(int nStudents) {
		return getStudents(nStudents, true);
	}

	@Override
	public List<IdNameMarks> getWorstStudents(int nStudents) {
		return getStudents(nStudents, false);
	}

	@Override
	public List<IdNameMarks> getBestStudentsSubject(int nStudents, String subject) {
		AddFieldsOperation addFieldsOperation = addFields().addFieldWithValue("mrks", "$marks").build();
		UnwindOperation unwindOperation = unwind("marks", true);
		GroupOperation groupOperation = group("id", "name").avg("marks.score").as(AVG_SCORE_FIELD).addToSet("$marks")
				.as("mrks");
		double avgScoreSubject = getStudentsAvgScoreSubject(subject);
		MatchOperation matchOperation2 = match(Criteria.where(AVG_SCORE_FIELD).gt(avgScoreSubject));
		SortOperation sortOperation = sort(Direction.DESC, AVG_SCORE_FIELD);
		ProjectionOperation projectionOperation = project().andExclude(AVG_SCORE_FIELD);
		LimitOperation limitOperation = limit(nStudents);
		Aggregation pipeline = newAggregation(List.of(addFieldsOperation, unwindOperation, groupOperation,
				matchOperation2, sortOperation, projectionOperation, projectionOperation, limitOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> resultDocuments = aggregationResult.getMappedResults();
		return resultDocuments.stream().map(this::toIdNameMarks).toList();
	}

	@Override
	public List<MarksBucket> scoresDistribution(int nBuckets) {
		UnwindOperation unwindOperation = unwind("marks");
		BucketAutoOperation bucketAutoOperation = bucketAuto("marks.score", nBuckets);
		Aggregation pipeline = newAggregation(List.of(unwindOperation, bucketAutoOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> resultDocuments = aggregationResult.getMappedResults();
		return resultDocuments.stream().map(this::toMarksBucket).toList();
	}

	private List<IdNameMarks> getStudents(int nStudents, boolean isBest) {

		AddFieldsOperation addFieldsOperation = addFields().addField(AVG_SCORE_FIELD)
				.withValueOf(MongoExpression.create("{$ifNull: [{$avg: '$marks.score'}, 0]}")).build();
		double score = getStudentsAvgScore();
		MatchOperation matchOperation = match(
				isBest ? Criteria.where(AVG_SCORE_FIELD).gt(score) : Criteria.where(AVG_SCORE_FIELD).lt(score));
		SortOperation sortOperation = sort(isBest ? Direction.DESC : Direction.ASC, AVG_SCORE_FIELD);
		ProjectionOperation projectionOperation = project().andExclude(AVG_SCORE_FIELD);
		LimitOperation limitOperation = limit(nStudents);
		Aggregation pipeline = newAggregation(List.of(addFieldsOperation, matchOperation, sortOperation,
				projectionOperation, projectionOperation, limitOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> resultDocuments = aggregationResult.getMappedResults();
		return resultDocuments.stream().map(this::toIdNameMarks).toList();
	}

	private double getStudentsAvgScoreSubject(String subject) {
		UnwindOperation unwindOperation = unwind("marks");
		MatchOperation matchOperation = match(Criteria.where("marks.subject").is(subject));
		GroupOperation groupOperation = group().avg("marks.score").as(AVG_SCORE_FIELD);
		Aggregation pipeline = newAggregation(List.of(unwindOperation, matchOperation, groupOperation));
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		double res = aggregationResult.getUniqueMappedResult().getDouble(AVG_SCORE_FIELD);
		return res;
	}

	IdNameMarks toIdNameMarks(StudentDoc studentDoc) {
		return new IdNameMarks() {

			@Override
			public String getName() {
				return studentDoc.getName();
			}

			@Override
			public long getId() {
				return studentDoc.getId();
			}

			@Override
			public List<MarkDto> getMarks() {
				return studentDoc.getMarks();
			}
		};
	}

	IdNameMarks toIdNameMarks(Document document) {
		return new IdNameMarks() {

			@Override
			public String getName() {
				return document.getString("name");
			}

			@Override
			public long getId() {
				return document.getLong("_id");
			}

			@Override
			public List<MarkDto> getMarks() {
				List<Document> res = document.getList("marks", Document.class);
				return res.stream().map(d -> toMarkDto(d)).toList();
			}
		};
	}

	MarkDto toMarkDto(Document document) {
		return new MarkDto(document.getString("subject"),
				document.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
				document.getInteger("score"));
	}

	MarksBucket toMarksBucket(Document document) {
		Document idDocument = document.get("_id", Document.class);
		return new MarksBucket(idDocument.getInteger("min"), idDocument.getInteger("max"),
				document.getInteger("count"));
	}

}
