package com.spherelabs.utils;

import com.spherelabs.error.Failure;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Eithers {

  public static <T> Either<Failure, T> of(Supplier<T> function) {
    return Try.of(function::get)
        .toEither()
        .mapLeft(ErrorUtil::handleError);
  }

  public static <T, R>  Either<Failure, R> of(T input, Function<T, R> function) {
    return Try.of(() -> function.apply(input))
        .toEither()
        .mapLeft(ErrorUtil::handleError);
  }

  public static <T, L, R>  Either<Failure, R> of(T input1, L input2, BiFunction<T, L,  R> function) {
    return Try.of(() -> function.apply(input1, input2))
        .toEither()
        .mapLeft(ErrorUtil::handleError);
  }
}
