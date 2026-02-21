package ru.kolivim.document.workflow.utils.specification;

import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;

public class SpecificationUtils {


    public static <T> Specification<T> like(String key, String value) {
        return (root, query, criteriaBuilder) -> value == null
                ? null : criteriaBuilder.like(criteriaBuilder.lower(root.get(key)), "%" + value.toLowerCase() + "%");
    }


    public static <T, K> Specification<T> equal(String key, K value) {
        return (root, query, criteriaBuilder) -> value == null
                ? null : criteriaBuilder.equal(root.get(key), value);
    }

    public static <T, K> Specification<T>notEqual(String key, K value) {
        return (root, query, criteriaBuilder) -> value == null
                ? null : criteriaBuilder.notEqual(root.get(key), value);
    }


    public static <T, K> Specification<T> in(String key, K value) {
        Specification<T> spec = (root, query, criteriaBuilder) -> value == null
                ? null : criteriaBuilder.in(root.get(key)).value(value);
        return spec;
    }


    public static <T, K> Specification<T> between(String key, Integer ageFrom,  Integer ageTo) {
        Specification<T> spec = null;
        if((ageFrom==null)&(ageTo!=null)){
            spec = (root, query, criteriaBuilder) ->  criteriaBuilder.greaterThan(root.get(key),  ZonedDateTime.now().minusYears((int)ageTo));
        }
        else if(((ageFrom!=null)&(ageTo==null))){
            spec = (root, query, criteriaBuilder) ->  criteriaBuilder.lessThan(root.get(key),  ZonedDateTime.now().minusYears((int)ageFrom));
        }
        else if(((ageFrom!=null)&(ageTo!=null))) {
            spec = (root, query, criteriaBuilder) ->  criteriaBuilder.between(root.get(key), ZonedDateTime.now().minusYears((int) ageTo), ZonedDateTime.now().minusYears((int) ageFrom));
        }
        return spec;
    }


    public static <T, K> Specification <T> betweenDate(String key, ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo){
        Specification<T> spec = ((root, query, criteriaBuilder) -> dateTimeFrom == null || dateTimeTo == null
                ? null : criteriaBuilder.between(root.get(key), dateTimeFrom, dateTimeTo));
        return spec;
    }

}