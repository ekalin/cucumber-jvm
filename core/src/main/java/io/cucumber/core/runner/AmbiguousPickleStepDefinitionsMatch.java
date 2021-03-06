package io.cucumber.core.runner;

import gherkin.pickles.PickleStep;
import io.cucumber.core.api.Scenario;
import io.cucumber.core.stepexpression.Argument;

import java.util.Collections;

final class AmbiguousPickleStepDefinitionsMatch extends PickleStepDefinitionMatch {
    private final AmbiguousStepDefinitionsException exception;

    AmbiguousPickleStepDefinitionsMatch(String uri, PickleStep step, AmbiguousStepDefinitionsException e) {
        super(Collections.<Argument>emptyList(), new NoStepDefinition(), uri, step);
        this.exception = e;
    }

    @Override
    public void runStep(Scenario scenario) {
        throw exception;
    }

    @Override
    public void dryRunStep(Scenario scenario) {
        runStep(scenario);
    }

    @Override
    public Match getMatch() {
        return exception.getMatches().get(0);
    }
}
