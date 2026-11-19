package cz.vsb.fei.DonkeyKongFX.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Values every page needs, such as the address of the database service shown in the footer links.
 */
@ControllerAdvice(assignableTypes = {DashboardUIController.class, GameResultUIController.class})
public class GlobalModelAttributes {

    @Value("${donkeykong.db.base-url}")
    private String dbUrl;

    @ModelAttribute("dbUrl")
    public String dbUrl() {
        return dbUrl.endsWith("/") ? dbUrl.substring(0, dbUrl.length() - 1) : dbUrl;
    }
}
