package order;

import static org.hamcrest.CoreMatchers.notNullValue;

import io.qameta.allure.Step;
import io.qameta.allure.internal.shadowed.jackson.core.JsonProcessingException;
import io.qameta.allure.internal.shadowed.jackson.databind.ObjectMapper;
import config.Config;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import user.User;
import user.UserSpec;

import java.util.ArrayList;

public class OrderSpec {

    private static final String INGREDIENTS = "/ingredients";
    private static final String ORDERS = "/orders";
    private static String jsonString;
    static ObjectMapper mapper = new ObjectMapper();

    @Step("получение данных об ингредиентах")
    public static ValidatableResponse getResponseRequestIngredients() throws JsonProcessingException {
        return RestAssured.given().log().all()
                .baseUri(Config.BASE_URL)
                .get(INGREDIENTS)
                .then().log().all()
                .statusCode(200);
    }

    @Step("Создание заказа")
    public static ValidatableResponse getResponseCreateOrder(Order order, String userAccessToken, int statusCode) throws JsonProcessingException {
        jsonString = mapper.writeValueAsString(order);
        return RestAssured.given().log().all()
                .headers("Authorization", userAccessToken, "Content-Type", "application/json")
                .baseUri(Config.BASE_URL)
                .body(jsonString)
                .when()
                .post(ORDERS)
                .then().log().all()
                .statusCode(statusCode);
    }

    @Step("Создание списка валидных хешей ингредиентов")
    public static ArrayList<String> getCreatedListOfValidHashesOfIngredients() throws JsonProcessingException {
        ArrayList<String> ingredientsHash = new ArrayList<>(OrderSpec.getResponseRequestIngredients()
                .extract()
                .path("data._id"));
        return ingredientsHash;
    }

    @Step("Создание списка заказов пользователя")
    public static void createListOfOrders(User user, int numberOfOrders) throws JsonProcessingException {
        ArrayList<String> ingredientsHash = getCreatedListOfValidHashesOfIngredients();
        String[] ingredients = new String[]{ingredientsHash.get(0), ingredientsHash.get(ingredientsHash.size() - 1)};
        Order order = new Order(ingredients);
        UserSpec response = UserSpec.getResponseUserAuthorization(user, 200);
        for (int i = 0; i < numberOfOrders; i++) {
            OrderSpec.getResponseCreateOrder(order, response.accessToken, 200)
                    .assertThat()
                    .body("order.number", notNullValue());
        }

        UserSpec.getResponseLogoutUser(response.refreshToken, 200);
    }

    @Step("Получение списка заказов")
    public static ValidatableResponse getAnOrderListRequestResponse(String userAccessToken, int statusCode) {
        return RestAssured.given().log().all()
                .header("Authorization", userAccessToken)
                .baseUri(Config.BASE_URL)
                .when()
                .get(ORDERS)
                .then().log().all()
                .statusCode(statusCode);
    }
}