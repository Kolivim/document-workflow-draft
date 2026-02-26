package ru.kolivim.document.workflow.util.specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.kolivim.document.workflow.entity.Document;

import java.time.ZonedDateTime;
import java.util.List;

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


    /** Хранящееся в переданном поле значение даты попадает в переданный диапазон дат */
    public static <T, K> Specification <T> betweenDate(String key, ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo){
        Specification<T> spec = ((root, query, criteriaBuilder) -> dateTimeFrom == null || dateTimeTo == null
                ? null : criteriaBuilder.between(root.get(key), dateTimeFrom, dateTimeTo));
        return spec;
    }


    /** Прямое сравнение: дата равна (EQUAL) */
    public static <T> Specification<T> equalDate(String field, ZonedDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get(field), dateTime);
        };
    }


    /** Спецификация для поиска по списку Id */
    public static <T> Specification<T> listIn(List<Long> idList) {
        return (root, query, criteriaBuilder) -> {
            if (idList == null || idList.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("id").in(idList);
        };
    }


    /** Спецификация для получения существующих ID из списка */
    public static Specification<Document> existentIds(List<Long> idList) {
        return (root, query, cb) -> {
            query.select(root.get("id")).distinct(true);
            return root.get("id").in(idList);
        };
    }


    /** Спецификация для получения несуществующих ID через подзапрос */
    public static Specification<Document> nonExistentIds(List<Long> idList) {
        return (root, query, cb) -> {

            /** Подзапрос для получения существующих ID */
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Document> subRoot = subquery.from(Document.class);
            subquery.select(subRoot.get("id"))
                    .where(subRoot.get("id").in(idList));

            /** Возвращаем ID из списка, которых нет в подзапросе */
            return cb.and(
                    root.get("id").in(idList),
                    cb.not(root.get("id").in(subquery))
            );
        };

    }


}