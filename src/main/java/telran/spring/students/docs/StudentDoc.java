package telran.spring.students.docs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;
import telran.spring.students.dto.*;

@Document(collection = "studetns")
@Data
public class StudentDoc {

	final long id;
	@NonNull
	String name;
	@NonNull
	String phone;
	List<MarkDto> marks = new ArrayList<MarkDto>();

	public StudentDoc(long id, String name, String phone, List<MarkDto> marks) {
		this.id = id;
		this.name = name;
		this.phone = phone;
		this.marks = marks;
	}
	
	public static StudentDoc of(StudentDto student) {
		return new StudentDoc(student.id(), student.name(), student.phone(), null);
	}

	public StudentDto build() {
		return new StudentDto(id, name, phone);
	}

}
