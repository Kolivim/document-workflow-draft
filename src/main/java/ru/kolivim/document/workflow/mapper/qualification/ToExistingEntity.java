package ru.kolivim.document.workflow.mapper.qualification;

import org.mapstruct.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Квалификатор для методов маппера, которые обновляют существующую Entity */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ToExistingEntity {}
