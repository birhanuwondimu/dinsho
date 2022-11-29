package com.dinsho.solo.Repository;

import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface QueryByExampleExecutor<T> {
    <S extends T> Optional<S> findOne(Example<S> example);

    <S extends T> Iterable<S> findAll(Example<S> employeeExample);

    <S extends T> Iterable<S> findAll(Example<S> example, Sort sort);

    <S extends T> Page<S> findAll(Example<S> example, Pageable pageable);

    <S extends T> long count(Example<S> example);

    <S extends T> boolean exists(Example<S> example);
}