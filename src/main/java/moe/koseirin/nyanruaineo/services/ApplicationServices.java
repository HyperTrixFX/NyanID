package moe.koseirin.nyanruaineo.services;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.ApplicationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServices {
    private final ApplicationRepository applicationRepository;

    public ApplicationServices(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public ResponseEntity<?> CreateApp() {


        return null;
    }

}
