package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.ClassViolatingThirdPartyRules;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWithProblem;
import com.tngtech.archunit.example.thirdparty.ThirdPartyClassWorkaroundFactory;
import com.tngtech.archunit.lang.ArchCondition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.originOwner;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class ThirdPartyRulesTest {
    protected static final String THIRD_PARTY_CLASS_RULE_TEXT =
            "not instantiate " +
                    ThirdPartyClassWithProblem.class.getSimpleName() +
                    " and its subclasses, but instead use " +
                    ThirdPartyClassWorkaroundFactory.class.getSimpleName();

    private JavaClasses classes;

    @Before
    public void setUp() throws Exception {
        classes = new ClassFileImporter().importPackagesOf(ClassViolatingThirdPartyRules.class);
    }

    @Ignore
    @Test
    public void third_party_class_should_only_be_instantiated_via_workaround() {
        classes().should(notCreateProblematicClassesOutsideOfWorkaroundFactory()
                .as(THIRD_PARTY_CLASS_RULE_TEXT))
                .check(classes);
    }

    private ArchCondition<JavaClass> notCreateProblematicClassesOutsideOfWorkaroundFactory() {
        DescribedPredicate<JavaCall<?>> constructorCallOfThirdPartyClass =
                target(is(constructor())).and(targetOwner(is(assignableTo(ThirdPartyClassWithProblem.class))));

        DescribedPredicate<JavaCall<?>> notFromWithinThirdPartyClass =
                originOwner(is(not(assignableTo(ThirdPartyClassWithProblem.class)))).forSubType();

        DescribedPredicate<JavaCall<?>> notFromWorkaroundFactory =
                originOwner(is(not(equivalentTo(ThirdPartyClassWorkaroundFactory.class)))).forSubType();

        DescribedPredicate<JavaCall<?>> targetIsIllegalConstructorOfThirdPartyClass =
                constructorCallOfThirdPartyClass.
                        and(notFromWithinThirdPartyClass).
                        and(notFromWorkaroundFactory);

        return never(callCodeUnitWhere(targetIsIllegalConstructorOfThirdPartyClass));
    }
}
