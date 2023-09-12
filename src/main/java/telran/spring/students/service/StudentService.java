package telran.spring.students.service;

import java.time.LocalDate;
import java.util.List;

import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.*;

public interface StudentService {

	StudentDto addStudent(StudentDto studentDto);

	void addMark(Long studentId, MarkDto markDto);

	List<MarkDto> getMarksStudentSubject(long studentId, String subject);

	List<MarkDto> getMarksStudentDates(long id, LocalDate fromDate, LocalDate toDate);

	List<StudentDto> getStudentsPhonePrefix(String phone);

	List<IdName> getStudentsAllScoresGreater(int score);

	List<Long> removeStudentsWithFewMarks(int nMarks);

	List<IdName> getStudentsScoresSubjectGreater(int score, String subject);

	List<Long> removeStudentsNoLowMarks(int score);

	double getStudentsAvgScore();

	List<IdName> getGoodStudents(); // students having avg scores greater than good mark threshold

	List<IdName> getStudentsAvgMarkGreater(int score);

	List<IdNameMarks> findStudents(String jsonQuery);

	List<IdNameMarks> getBestStudents(int nStudents); // <nStudents> best students ('best' criteria is sum of all students marks)

	List<IdNameMarks> getWorstStudents(int nStudents); // <nStudents> worst students by same criteria

	List<IdNameMarks> getBestStudentsSubject(int nStudents, String subject); 
	
	List<MarksBucket> scoresDistribution(int nBuckets);

}
