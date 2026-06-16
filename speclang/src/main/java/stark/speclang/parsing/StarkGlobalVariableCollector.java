/*
 * STARK: Software Tool for the Analysis of Robustness in the unKnown environment
 *
 *              Copyright (C) 2023.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stark.speclang.parsing;

import stark.speclang.StarkSpecificationLanguageBaseVisitor;
import stark.speclang.StarkSpecificationLanguageParser;
import stark.speclang.variables.StarkVariableRegistry;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class StarkGlobalVariableCollector extends StarkSpecificationLanguageBaseVisitor<Boolean> {

    private final StarkVariableRegistry registry;

    private final ParseErrorCollector errors;


    public StarkGlobalVariableCollector(ParseErrorCollector errors, StarkVariableRegistry registry) {
        this.registry = registry;
        this.errors = errors;
    }


    @Override
    public Boolean visitDeclarationComponent(StarkSpecificationLanguageParser.DeclarationComponentContext ctx) {
        return recordVariables(ctx.variables);
    }

    private Boolean recordVariables(List<StarkSpecificationLanguageParser.VariableDeclarationContext> variables) {
        boolean flag = true;
        for(StarkSpecificationLanguageParser.VariableDeclarationContext v: variables) {
            flag &= recordVariable(v.name);
        }
        return flag;
    }

    private boolean recordVariable(Token v) {
        String variableName = v.getText();
        if (registry.isDeclared(variableName)) {
            errors.record(ParseUtil.duplicatedVariablesDeclaration(v));
            return false;
        } else {
            registry.record(variableName);
            return true;
        }
    }

    @Override
    public Boolean visitDeclarationVariables(StarkSpecificationLanguageParser.DeclarationVariablesContext ctx) {
        return recordVariables(ctx.variableDeclaration());
    }

    @Override
    public Boolean visitDeclarationConstant(StarkSpecificationLanguageParser.DeclarationConstantContext ctx) {
        return true;
    }

    @Override
    public Boolean visitDeclarationParameter(StarkSpecificationLanguageParser.DeclarationParameterContext ctx) {
        return true;
    }

    @Override
    public Boolean visitDeclarationFunction(StarkSpecificationLanguageParser.DeclarationFunctionContext ctx) {
        return true;
    }

    @Override
    public Boolean visitDeclarationPenalty(StarkSpecificationLanguageParser.DeclarationPenaltyContext ctx) {
        return true;
    }

    @Override
    public Boolean visitDeclarationEnvironmnet(StarkSpecificationLanguageParser.DeclarationEnvironmnetContext ctx) {
        return true;
    }

    @Override
    public Boolean visitDeclarationType(StarkSpecificationLanguageParser.DeclarationTypeContext ctx) {
        return true;
    }

    @Override
    protected Boolean defaultResult() {
        return true;
    }

    @Override
    protected Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
        return aggregate && nextResult;
    }
}

