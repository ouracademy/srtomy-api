package org.ouracademy.srtomy.auth0;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;


@RestController
@AllArgsConstructor
public class LalaController {
    Auth0Gateway auth0Gateway;

    @GetMapping("lala")
    AccessToken lala() {
        return auth0Gateway.getAccessToken();
    }
}
