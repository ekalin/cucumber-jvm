package io.cucumber.core.stepexpression;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableType;
import io.cucumber.datatable.TableCellByTypeTransformer;
import io.cucumber.datatable.TableEntryByTypeTransformer;
import io.cucumber.datatable.TableEntryTransformer;
import io.cucumber.datatable.TableTransformer;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

public class StepExpressionFactoryTest {

    private static final TypeResolver UNKNOWN_TYPE = () -> Object.class;

    static class Ingredient {
        public String name;
        public Integer amount;
        public String unit;

        Ingredient() {
        }
    }

    private final TypeRegistry registry = new TypeRegistry(Locale.ENGLISH);
    private final List<List<String>> table = asList(asList("name", "amount", "unit"), asList("chocolate", "2", "tbsp"));
    private final List<List<String>> tableTransposed = asList(asList("name", "chocolate"), asList("amount", "2"), asList("unit", "tbsp"));


    private TableEntryTransformer<Ingredient> listBeanMapper(final TypeRegistry registry) {
        //Just pretend this is a bean mapper.
        return tableRow -> {
            Ingredient bean = new Ingredient();
            bean.amount = Integer.valueOf(tableRow.get("amount"));
            bean.name = tableRow.get("name");
            bean.unit = tableRow.get("unit");
            return bean;
        };
    }


    private TableTransformer<Ingredient> beanMapper(final TypeRegistry registry) {
        return table -> {
            Map<String, String> tableRow = table.transpose().asMaps().get(0);
            return listBeanMapper(registry).transform(tableRow);
        };
    }


    @Test
    public void table_expression_with_type_creates_table_from_table() {

        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", DataTable.class);


        List<Argument> match = expression.match("Given some stuff:", table);

        DataTable dataTable = (DataTable) match.get(0).getValue();
        assertThat(dataTable.cells(), is(equalTo(table)));
    }

    @Test
    public void table_expression_with_type_creates_single_ingredients_from_table() {

        registry.defineDataTableType(new DataTableType(Ingredient.class, beanMapper(registry)));
        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", Ingredient.class);
        List<Argument> match = expression.match("Given some stuff:", tableTransposed);


        Ingredient ingredient = (Ingredient) match.get(0).getValue();
        assertThat(ingredient.name, is(equalTo("chocolate")));
    }

    @Test
    public void table_expression_with_list_type_creates_list_of_ingredients_from_table() {

        registry.defineDataTableType(new DataTableType(Ingredient.class, listBeanMapper(registry)));

        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", getTypeFromStepDefinition());
        List<Argument> match = expression.match("Given some stuff:", table);

        List<Ingredient> ingredients = (List<Ingredient>) match.get(0).getValue();
        Ingredient ingredient = ingredients.get(0);
        assertThat(ingredient.amount, is(equalTo(2)));
    }

    @Test
    public void unknown_target_type_does_no_transform_data_table() {
        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", UNKNOWN_TYPE);
        List<Argument> match = expression.match("Given some stuff:", table);
        assertThat(match.get(0).getValue(), is(equalTo(DataTable.create(table))));
    }

    @Test
    public void unknown_target_type_does_not_transform_doc_string() {
        String docString = "A rather long and boring string of documentation";
        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", UNKNOWN_TYPE);
        List<Argument> match = expression.match("Given some stuff:", docString);
        assertThat(match.get(0).getValue(), is(equalTo(docString)));
    }

    @Test
    public void empty_table_cells_are_presented_as_null_to_transformer() {
        registry.setDefaultDataTableEntryTransformer(new TableEntryByTypeTransformer() {
            @Override
            public <T> T transform(Map<String, String> map, Class<T> aClass, TableCellByTypeTransformer tableCellByTypeTransformer) {
                return new ObjectMapper().convertValue(map, aClass);
            }
        });

        StepExpression expression = new StepExpressionFactory(registry).createExpression("Given some stuff:", getTypeFromStepDefinition());
        List<List<String>> table = asList(asList("name", "amount", "unit"), asList("chocolate", null, "tbsp"));
        List<Argument> match = expression.match("Given some stuff:", table);

        List<Ingredient> ingredients = (List<Ingredient>) match.get(0).getValue();
        Ingredient ingredient = ingredients.get(0);
        assertThat(ingredient.name, is(equalTo("chocolate")));

    }

    private Type getTypeFromStepDefinition() {
        for (Method method : this.getClass().getMethods()) {
            if (method.getName().equals("fake_step_definition")) {
                return method.getGenericParameterTypes()[0];
            }
        }
        throw new IllegalStateException();
    }


    @SuppressWarnings("unused")
    public void fake_step_definition(List<Ingredient> ingredients) {

    }

}
