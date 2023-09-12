package telran.spring.students;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.IdName;
import telran.spring.students.dto.IdNameMarks;
import telran.spring.students.dto.MarkDto;
import telran.spring.students.dto.MarksBucket;
import telran.spring.students.dto.StudentDto;
import telran.spring.students.dto.SubjectMark;
import telran.spring.students.repo.StudentRepository;
import telran.spring.students.service.StudentService;
import static telran.spring.students.TestDbCreation.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentsServiceTests {

	@Autowired
	StudentService studentService;
	@Autowired
	TestDbCreation testDbCreation;

	StudentRepository studentRepository;

	@BeforeEach
	void setUp() {
		testDbCreation.createDB();
	}

	@Test
	void studentSubjectMarks() {
		List<MarkDto> marks = studentService.getMarksStudentSubject(ID1, SUBJECT1);
		assertEquals(1, marks.size());
		MarkDto mark = marks.get(0);
		assertEquals(80, mark.score());
		assertEquals(SUBJECT1, mark.subject());
	}

	@Test
	void studentDatesMarksTest() {
		List<MarkDto> marks = studentService.getMarksStudentDates(ID1, DATE2, DATE3);
		assertEquals(2, marks.size());
	}

	@Test
	void studentPrefixTest() {
		List<StudentDto> students = studentService.getStudentsPhonePrefix("050");
		assertEquals(3, students.size());
		StudentDto student2 = students.get(0);
		// нет оценок, тк не включено в результат запроса поле marks
		// (чтобы включить, надо над запросом в StudentRepository поставить marks:1)
		assertEquals(ID2, student2.id());
		students.forEach(s -> assertTrue(s.phone().startsWith("050")));
	}

	@Test
	void studentsAllMarksGreaterTest() {
		List<IdName> students = studentService.getStudentsAllScoresGreater(70);
		assertEquals(2, students.size());
		IdName studentDoc = students.get(0);
		assertEquals(ID3, studentDoc.getId());
		assertEquals("name3", studentDoc.getName());
		assertEquals(ID5, students.get(1).getId());
	}

	@Test
	void studentsFewMarksTest() {
		List<Long> ids = studentService.removeStudentsWithFewMarks(2);
		assertEquals(2, ids.size());
		assertEquals(ID4, ids.get(0));
		assertEquals(ID6, ids.get(1));
		assertNull(studentRepository.findById(ID4).orElse(null));
		assertNull(studentRepository.findById(ID6).orElse(null));
	}

	@Test
	void studentsScoresSubjectGreaterTest() {
		List<IdName> students = studentService.getStudentsScoresSubjectGreater(90, SUBJECT3);
		assertEquals(1, students.size());
		assertEquals(ID3, students.get(0).getId());
		List<IdName> students2 = studentService.getStudentsScoresSubjectGreater(89, SUBJECT3);
		assertEquals(2, students2.size());
		assertEquals(ID3, students2.get(0).getId());
		assertEquals(ID5, students2.get(1).getId());
		List<IdName> students3 = studentService.getStudentsScoresSubjectGreater(88, SUBJECT3);
		assertEquals(3, students3.size());
		assertEquals(ID1, students3.get(0).getId());
		assertEquals(ID3, students3.get(1).getId());
		assertEquals(ID5, students3.get(2).getId());
	}

	@Test
	void studentsNoLowMarksTest() {
		List<Long> students = studentService.removeStudentsNoLowMarks(71);
		assertEquals(2, students.size());
		assertEquals(ID4, students.get(0));
		assertEquals(ID6, students.get(1));
	}

	@Test
	void getAvgMarksTest() {
		assertEquals(testDbCreation.getAvgMark(), studentService.getStudentsAvgScore(), 0.1);
	}

	@Test
	void getStudentsAvgMarkGreaterTest() {
		List<IdName> idNamesGood = studentService.getGoodStudents();
		List<IdName> idNamesGreater = studentService.getStudentsAvgMarkGreater(75);
		idNamesGood.forEach(in -> assertTrue(testDbCreation.getAvgMarkStudent(in.getId()) > 75));
		assertEquals(3, idNamesGood.size());
		assertEquals(ID3, idNamesGood.get(0).getId());
		assertEquals(ID5, idNamesGood.get(1).getId());
		assertEquals(ID2, idNamesGood.get(2).getId());
		assertEquals(idNamesGood.size(), idNamesGreater.size());
	}

	@Test
	void findQueryTest() {
		List<IdNameMarks> actualRes = studentService.findStudents("{phone:{$regex:/^050/}}");
		List<StudentDto> expectedRes = studentService.getStudentsPhonePrefix("050");
		assertEquals(expectedRes.size(), actualRes.size());
		IdNameMarks actual1 = actualRes.get(0);
		StudentDto expected1 = expectedRes.get(0);
		assertEquals(expected1.id(), actual1.getId());
	}

	@Test
	void bestNStundetsTest() {
		List<IdNameMarks> students = studentService.getBestStudents(3);
		assertEquals(3, students.size());
		assertEquals(ID3, students.get(0).getId());
		assertEquals(4, students.get(0).getMarks().size());
		assertEquals(ID5, students.get(1).getId());
		assertEquals(3, students.get(1).getMarks().size());
		assertEquals(ID2, students.get(2).getId());
		assertEquals(2, students.get(2).getMarks().size());
	}

	@Test
	void worstNStundetsTest() {
		List<IdNameMarks> students = studentService.getWorstStudents(3);
		assertEquals(2, students.size());
		assertEquals(ID6, students.get(0).getId());
		assertEquals(0, students.get(0).getMarks().size());
		assertEquals(ID1, students.get(1).getId());
		assertEquals(4, students.get(1).getMarks().size());
	}

	@Test
	void bestStudentsSubject() {
		List<IdNameMarks> students = studentService.getBestStudentsSubject(3, SUBJECT1);
		assertEquals(3, students.size());
		assertEquals(ID3, students.get(0).getId());
		assertEquals(4, students.get(0).getMarks().size());
		assertEquals(ID5, students.get(1).getId());
		assertEquals(3, students.get(1).getMarks().size());
		assertEquals(ID2, students.get(2).getId());
		assertEquals(2, students.get(2).getMarks().size());
	}

	@Test
	void bucketsTest() {
		List<MarksBucket> marks = studentService.scoresDistribution(3);
		assertEquals(3, marks.size());
	}
	
	

}
