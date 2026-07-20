package recruitment.dev.applicationservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import recruitment.dev.applicationservice.entities.CV;

import java.util.List;

public interface CVRepository extends JpaRepository<CV, Long> {

}
