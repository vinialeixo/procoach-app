package com.fitness.procoach.library.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Referência bibliográfica embutida na entidade JPA do exercício. */
@Embeddable
public class ExerciseReferenceJpa {

	@Column(name = "authors", nullable = false)
	private String authors;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "source", nullable = false)
	private String source;

	@Column(name = "year")
	private Integer year;

	protected ExerciseReferenceJpa() {
	}

	public ExerciseReferenceJpa(String authors, String title, String source, Integer year) {
		this.authors = authors;
		this.title = title;
		this.source = source;
		this.year = year;
	}

	public String getAuthors() {
		return authors;
	}

	public String getTitle() {
		return title;
	}

	public String getSource() {
		return source;
	}

	public Integer getYear() {
		return year;
	}
}
