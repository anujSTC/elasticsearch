/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.function;

import org.elasticsearch.common.Strings;
import org.elasticsearch.xpack.sql.SqlIllegalArgumentException;
import org.elasticsearch.xpack.sql.expression.Expression;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Avg;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Correlation;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Count;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Covariance;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Kurtosis;
import org.elasticsearch.xpack.sql.expression.function.aggregate.MatrixCount;
import org.elasticsearch.xpack.sql.expression.function.aggregate.MatrixMean;
import org.elasticsearch.xpack.sql.expression.function.aggregate.MatrixVariance;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Max;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Mean;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Min;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Percentile;
import org.elasticsearch.xpack.sql.expression.function.aggregate.PercentileRank;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Skewness;
import org.elasticsearch.xpack.sql.expression.function.aggregate.StddevPop;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Sum;
import org.elasticsearch.xpack.sql.expression.function.aggregate.SumOfSquares;
import org.elasticsearch.xpack.sql.expression.function.aggregate.VarPop;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.DayOfMonth;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.DayOfWeek;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.DayOfYear;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.HourOfDay;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.MinuteOfDay;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.MinuteOfHour;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.MonthOfYear;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.SecondOfMinute;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.Year;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.ACos;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.ASin;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.ATan;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Abs;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Cbrt;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Ceil;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Cos;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Cosh;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Degrees;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.E;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Exp;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Expm1;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Floor;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Log;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Log10;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Pi;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Radians;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Round;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Sin;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Sinh;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Sqrt;
import org.elasticsearch.xpack.sql.expression.function.scalar.math.Tan;
import org.elasticsearch.xpack.sql.parser.ParsingException;
import org.elasticsearch.xpack.sql.tree.Location;
import org.elasticsearch.xpack.sql.util.StringUtils;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class FunctionRegistry {
    private static final List<FunctionDefinition> DEFAULT_FUNCTIONS = unmodifiableList(Arrays.asList(
        // Aggregate functions
            def(Avg.class, Avg::new),
            def(Count.class, Count::new),
            def(Max.class, Max::new),
            def(Min.class, Min::new),
            def(Sum.class, Sum::new),
            // Statistics
            def(Mean.class, Mean::new),
            def(StddevPop.class, StddevPop::new),
            def(VarPop.class, VarPop::new),
            def(Percentile.class, Percentile::new),
            def(PercentileRank.class, PercentileRank::new),
            def(SumOfSquares.class, SumOfSquares::new),
            // Matrix aggs
            def(MatrixCount.class, MatrixCount::new),
            def(MatrixMean.class, MatrixMean::new),
            def(MatrixVariance.class, MatrixVariance::new),
            def(Skewness.class, Skewness::new),
            def(Kurtosis.class, Kurtosis::new),
            def(Covariance.class, Covariance::new),
            def(Correlation.class, Correlation::new),
        // Scalar functions
            // Date
            def(DayOfMonth.class, DayOfMonth::new, "DAY", "DOM"),
            def(DayOfWeek.class, DayOfWeek::new, "DOW"),
            def(DayOfYear.class, DayOfYear::new, "DOY"),
            def(HourOfDay.class, HourOfDay::new, "HOUR"),
            def(MinuteOfDay.class, MinuteOfDay::new),
            def(MinuteOfHour.class, MinuteOfHour::new, "MINUTE"),
            def(SecondOfMinute.class, SecondOfMinute::new, "SECOND"),
            def(MonthOfYear.class, MonthOfYear::new, "MONTH"),
            def(Year.class, Year::new),
            // Math
            def(Abs.class, Abs::new),
            def(ACos.class, ACos::new),
            def(ASin.class, ASin::new),
            def(ATan.class, ATan::new),
            def(Cbrt.class, Cbrt::new),
            def(Ceil.class, Ceil::new),
            def(Cos.class, Cos::new),
            def(Cosh.class, Cosh::new),
            def(Degrees.class, Degrees::new),
            def(E.class, E::new),
            def(Exp.class, Exp::new),
            def(Expm1.class, Expm1::new),
            def(Floor.class, Floor::new),
            def(Log.class, Log::new),
            def(Log10.class, Log10::new),
            def(Pi.class, Pi::new),
            def(Radians.class, Radians::new),
            def(Round.class, Round::new),
            def(Sin.class, Sin::new),
            def(Sinh.class, Sinh::new),
            def(Sqrt.class, Sqrt::new),
            def(Tan.class, Tan::new),
        // Special
            def(Score.class, Score::new)));

    private final Map<String, FunctionDefinition> defs = new LinkedHashMap<>();
    private final Map<String, String> aliases;

    /**
     * Constructor to build with the default list of functions.
     */
    public FunctionRegistry() {
        this(DEFAULT_FUNCTIONS);
    }

    /**
     * Constructor specifying alternate functions for testing.
     */
    FunctionRegistry(List<FunctionDefinition> functions) {
        this.aliases = new HashMap<>();
        for (FunctionDefinition f : functions) {
            defs.put(f.name(), f);
            for (String alias : f.aliases()) {
                Object old = aliases.put(alias, f.name());
                if (old != null) {
                    throw new IllegalArgumentException("alias [" + alias + "] is used by [" + old + "] and [" + f.name() + "]");
                }
                defs.put(alias, f);
            }
        }
    }

    public Function resolveFunction(UnresolvedFunction ur, DateTimeZone timeZone) {
        FunctionDefinition def = defs.get(normalize(ur.name()));
        if (def == null) {
            throw new SqlIllegalArgumentException("Cannot find function %s; this should have been caught during analysis", ur.name());
        }
        return def.builder().apply(ur, timeZone);
    }

    public String concreteFunctionName(String alias) {
        String normalized = normalize(alias);
        return aliases.getOrDefault(normalized, normalized);
    }

    public boolean functionExists(String name) {
        return defs.containsKey(normalize(name));
    }

    public Collection<FunctionDefinition> listFunctions() {
        return defs.entrySet().stream()
                .map(e -> new FunctionDefinition(e.getKey(), emptyList(), e.getValue().clazz(), e.getValue().builder()))
                .collect(toList());
    }

    public Collection<FunctionDefinition> listFunctions(String pattern) {
        Pattern p = Strings.hasText(pattern) ? Pattern.compile(normalize(pattern)) : null;
        return defs.entrySet().stream()
                .filter(e -> p == null || p.matcher(e.getKey()).matches())
                .map(e -> new FunctionDefinition(e.getKey(), emptyList(), e.getValue().clazz(), e.getValue().builder()))
                .collect(toList());
    }

    /**
     * Build a {@linkplain FunctionDefinition} for a no-argument function that
     * is not aware of time zone and does not support {@code DISTINCT}.
     */
    static <T extends Function> FunctionDefinition def(Class<T> function,
            java.util.function.Function<Location, T> ctorRef, String... aliases) {
        FunctionBuilder builder = (location, children, distinct, tz) -> {
            if (false == children.isEmpty()) {
                throw new IllegalArgumentException("expects no arguments");
            }
            if (distinct) {
                throw new IllegalArgumentException("does not support DISTINCT yet it was specified");
            }
            return ctorRef.apply(location);
        };
        return def(function, builder, aliases);
    }

    /**
     * Build a {@linkplain FunctionDefinition} for a unary function that is not
     * aware of time zone and does not support {@code DISTINCT}.
     */
    @SuppressWarnings("overloads")  // These are ambiguous if you aren't using ctor references but we always do
    static <T extends Function> FunctionDefinition def(Class<T> function,
            BiFunction<Location, Expression, T> ctorRef, String... aliases) {
        FunctionBuilder builder = (location, children, distinct, tz) -> {
            if (children.size() != 1) {
                throw new IllegalArgumentException("expects exactly one argument");
            }
            if (distinct) {
                throw new IllegalArgumentException("does not support DISTINCT yet it was specified");
            }
            return ctorRef.apply(location, children.get(0));
        };
        return def(function, builder, aliases);
    }

    /**
     * Build a {@linkplain FunctionDefinition} for a unary function that is not
     * aware of time zone but does support {@code DISTINCT}.
     */
    @SuppressWarnings("overloads")  // These are ambiguous if you aren't using ctor references but we always do
    static <T extends Function> FunctionDefinition def(Class<T> function,
            DistinctAwareUnaryFunctionBuilder<T> ctorRef, String... aliases) {
        FunctionBuilder builder = (location, children, distinct, tz) -> {
            if (children.size() != 1) {
                throw new IllegalArgumentException("expects exactly one argument");
            }
            return ctorRef.build(location, children.get(0), distinct);
        };
        return def(function, builder, aliases);
    }
    interface DistinctAwareUnaryFunctionBuilder<T> {
        T build(Location location, Expression target, boolean distinct);
    }

    /**
     * Build a {@linkplain FunctionDefinition} for a unary function that is
     * aware of time zone and does not support {@code DISTINCT}.
     */
    @SuppressWarnings("overloads")  // These are ambiguous if you aren't using ctor references but we always do
    static <T extends Function> FunctionDefinition def(Class<T> function,
            TimeZoneAwareUnaryFunctionBuilder<T> ctorRef, String... aliases) {
        FunctionBuilder builder = (location, children, distinct, tz) -> {
            if (children.size() != 1) {
                throw new IllegalArgumentException("expects exactly one argument");
            }
            if (distinct) {
                throw new IllegalArgumentException("does not support DISTINCT yet it was specified");
            }
            return ctorRef.build(location, children.get(0), tz);
        };
        return def(function, builder, aliases);
    }
    interface TimeZoneAwareUnaryFunctionBuilder<T> {
        T build(Location location, Expression target, DateTimeZone tz);
    }

    /**
     * Build a {@linkplain FunctionDefinition} for a binary function that is
     * not aware of time zone and does not support {@code DISTINCT}.
     */
    @SuppressWarnings("overloads")  // These are ambiguous if you aren't using ctor references but we always do
    static <T extends Function> FunctionDefinition def(Class<T> function,
            BinaryFunctionBuilder<T> ctorRef, String... aliases) {
        FunctionBuilder builder = (location, children, distinct, tz) -> {
            if (children.size() != 2) {
                throw new IllegalArgumentException("expects exactly two arguments");
            }
            if (distinct) {
                throw new IllegalArgumentException("does not support DISTINCT yet it was specified");
            }
            return ctorRef.build(location, children.get(0), children.get(1));
        };
        return def(function, builder, aliases);
    }
    interface BinaryFunctionBuilder<T> {
        T build(Location location, Expression lhs, Expression rhs);
    }

    private static FunctionDefinition def(Class<? extends Function> function, FunctionBuilder builder, String... aliases) {
        String primaryName = normalize(function.getSimpleName());
        BiFunction<UnresolvedFunction, DateTimeZone, Function> realBuilder = (uf, tz) -> {
            try {
                return builder.build(uf.location(), uf.children(), uf.distinct(), tz);
            } catch (IllegalArgumentException e) {
                throw new ParsingException("error building [" + primaryName + "]: " + e.getMessage(), e,
                        uf.location().getLineNumber(), uf.location().getColumnNumber());
            }
        };
        return new FunctionDefinition(primaryName, unmodifiableList(Arrays.asList(aliases)), function, realBuilder);
    }
    private interface FunctionBuilder {
        Function build(Location location, List<Expression> children, boolean distinct, DateTimeZone tz);
    }

    private static String normalize(String name) {
        // translate CamelCase to camel_case
        return StringUtils.camelCaseToUnderscore(name);
    }
}
