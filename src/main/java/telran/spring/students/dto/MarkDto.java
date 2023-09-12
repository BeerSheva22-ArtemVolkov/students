package telran.spring.students.dto;

import java.time.LocalDate;

public record MarkDto(String subject, LocalDate date, int score) {

}
