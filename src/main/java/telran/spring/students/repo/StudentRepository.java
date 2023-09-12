package telran.spring.students.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.IdName;
import telran.spring.students.dto.SubjectMark;

public interface StudentRepository extends MongoRepository<StudentDoc, Long> {

	SubjectMark findByIdAndMarksSubjectEquals(Long id, String subject);

	SubjectMark findByIdAndMarksDateBetween(Long id, LocalDate date1, LocalDate date2);
	
	// ?0 - подставляется нулевой аргумент из входных параметров метода
	// 1 - нужно поле в результат запроса, 0 - не включать в результат запроса
	@Query(value = "{phone:{$regex:/^?0/}}", fields = "{phone:1, name:1}") 
	List<StudentDoc> findStudentsPhonePrefix(String phonePrefix);

	@Query("{"
			+ "$and:["
				+ "{marks:"
					+ "{$elemMatch:"
						+ "{score:"
							+ "{$gt:?0}"
						+ "}"
					+ "}"
				+ "}, {marks:"
					+ "{$not:"
						+ "{$elemMatch:"
							+ "{score:"
								+ "{$lte:?0}"
							+ "}"
						+ "}"
					+ "}"
				+ "}]"
			+ "}")
	List<IdName> findStudentsAllMarksGreater(int score);

	@Query(value = "{$expr:{$lt:[{$size:$marks}, ?0]}}", fields = "{}", delete = true)
	List<StudentDoc> removeStudentsFewMarks(int nMarks);
	
	@Query(value = "{$and:["
				+ "{marks:{$elemMatch:{score:{$gt:?0}, subject:{$eq:?1}}}}, "
				+ "{marks:{$not:{$elemMatch:{score:{$lte:?0}, subject:{$eq:?1}}}}}"
			+ "]}")
	List<IdName> findStudentsScoresSubjectGreaterThan(int score, String Subject);
	
	@Query(value = "{$or: ["
				+ "{$expr: {$eq: [{$size:$marks}, 0]}}, "
				+ "{$and: ["
					+ "{marks: {$not: {$elemMatch: {score: {$gte: ?0}}}}}, "
					+ "{marks: {$elemMatch: {score: {$lt: ?0}}}}"
				+ "]}"
			+ "]}", fields = "{}", delete = true)
	List<StudentDoc> removeStudentsNoLowMarks(int score);
	
	
}
