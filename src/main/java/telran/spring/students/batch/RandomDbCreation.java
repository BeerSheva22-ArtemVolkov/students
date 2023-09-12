package telran.spring.students.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import telran.spring.students.docs.StudentDoc;
import telran.spring.students.dto.MarkDto;
import telran.spring.students.repo.StudentRepository;

@Component
@RequiredArgsConstructor
public class RandomDbCreation {

	final StudentRepository studentRepository;

	@Value("${app.random.students.amount:100}")
	int nStudents;

	@Value("${app.random.subjects.amount:5}")
	int nSubjects;

	@Value("${app.random.marks.min.amount:3}")
	int minMarks;

	@Value("${app.random.marks.max.amount:7}")
	int maxMarks;

	@Value("${app.random.creation.enable:false}")
	boolean creationEnable;

	@PostConstruct
	void createDb() {
		if (creationEnable) {
			List<StudentDoc> list = IntStream.rangeClosed(1, nStudents).mapToObj(this::getStudent).toList();
			studentRepository.saveAll(list);
		}
	}

	StudentDoc getStudent(int id) {
		String name = "name" + id;
		String phone = getRandomPhone();
		List<MarkDto> marks = getMarks();
		return new StudentDoc(id, name, phone, marks);
	}

	private String getRandomPhone() {
		String code = "05" + getRandomNumber(0, 10);
		int number = getRandomNumber(1000000, 10000000);
		return code + "-" + number;
	}

	private List<MarkDto> getMarks() {
		return Stream.generate(() -> getRandomMark()).limit(getRandomNumber(minMarks, maxMarks + 1)).toList();
	}

	private MarkDto getRandomMark() {
		String subject = "subject" + getRandomNumber(1, nSubjects + 1);
		LocalDate date = getRandomDate();
		return new MarkDto(subject, date, getRandomNumber(60, 101));
	}

	private LocalDate getRandomDate() {
		int year = getRandomNumber(2021, 2024);
		int month = getRandomNumber(1, 13);
		int day = getRandomNumber(1, 29);
		return LocalDate.of(year, month, day);
	}

	private int getRandomNumber(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max);
	}

}
