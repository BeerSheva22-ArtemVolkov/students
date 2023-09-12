package telran.spring.students;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.MarkDto;
import telran.spring.students.dto.StudentDto;
import telran.spring.students.repo.StudentRepository;

@Component
@RequiredArgsConstructor
public class TestDbCreation {

	final StudentRepository studentRepository;

	public static final Long ID1 = 123l;
	public static final Long ID2 = 124l;
	public static final Long ID3 = 125l;
	public static final Long ID4 = 126l;
	public static final Long ID5 = 127l;
	public static final Long ID6 = 128l;

	public static final String PHONE1 = "054-1231231";
	public static final String PHONE2 = "050-1234333";
	public static final String PHONE3 = "050-3333333";
	public static final String PHONE4 = "051-4444444";
	public static final String PHONE5 = "055-5555555";
	public static final String PHONE6 = "050-6666666";

	public static final String SUBJECT1 = "Java";
	public static final String SUBJECT2 = "JavaScript";
	public static final String SUBJECT3 = "HTML/CSS";
	public static final String SUBJECT4 = "React";

	public static final LocalDate DATE1 = LocalDate.parse("2023-08-10");
	public static final LocalDate DATE2 = LocalDate.parse("2023-08-15");
	public static final LocalDate DATE3 = LocalDate.parse("2023-08-25");
	public static final LocalDate DATE4 = LocalDate.parse("2023-09-01");


	StudentDto[] students = { 
			new StudentDto(ID1, "name1", PHONE1), new StudentDto(ID2, "name2", PHONE2),
			new StudentDto(ID3, "name3", PHONE3), new StudentDto(ID4, "name4", PHONE4),
			new StudentDto(ID5, "name5", PHONE5), new StudentDto(ID6, "name6", PHONE6) 
	};

	static MarkDto[][] marks = {
			{//ID1
				new MarkDto(SUBJECT1, DATE1, 1), 
				new MarkDto(SUBJECT2, DATE2, 0),
				new MarkDto(SUBJECT3, DATE3, 0),
				new MarkDto(SUBJECT3, DATE3, 0) 
			},
			{//ID2
				new MarkDto(SUBJECT1, DATE1, 71), 
				new MarkDto(SUBJECT2, DATE2, 80) 
			},
			{//ID3
				new MarkDto(SUBJECT1, DATE1, 80), 
				new MarkDto(SUBJECT2, DATE2, 75), 
				new MarkDto(SUBJECT3, DATE3, 95),
				new MarkDto(SUBJECT4, DATE4, 100) 
			},
			{//ID4
				new MarkDto(SUBJECT1, DATE1, 70) 
			},
			{//ID5
				new MarkDto(SUBJECT1, DATE1, 80), 
				new MarkDto(SUBJECT2, DATE2, 75),
				new MarkDto(SUBJECT3, DATE3, 90) 
			}, 
			{//ID6

			} 
	};
	
	void createDB() {
		studentRepository.deleteAll();
		List<StudentDoc> studentsDocsList = IntStream.range(0, students.length).mapToObj(this::indexToStudent).toList();
		studentRepository.saveAll(studentsDocsList);
	}
	
	StudentDoc indexToStudent(int index) {
		StudentDto student = students[index];
		StudentDoc res = new StudentDoc(student.id(), student.name(), student.phone(), null);
		res.setMarks(new ArrayList<>(List.of(marks[index])));
		return res;
	}
	
	double getAvgMark() {
		return Arrays.stream(marks).flatMap(Arrays::stream).collect(Collectors.averagingDouble(MarkDto::score));
	}
	
	double getAvgMarkStudent(long id) {
		return Arrays.stream(marks[(int)id - 123]).collect(Collectors.averagingDouble(MarkDto::score));
	}
	
}
