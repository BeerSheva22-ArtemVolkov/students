package telran.spring.students.dto;

import jakarta.validation.constraints.NotNull;

// @NotNull - аннотация для JSON, который придет от пользователя
public record StudentDto (@NotNull Long id, @NotNull String name, @NotNull String phone) {

}
