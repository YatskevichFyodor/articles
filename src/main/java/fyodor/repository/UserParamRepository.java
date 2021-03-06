package fyodor.repository;

import fyodor.model.UserParam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserParamRepository extends JpaRepository<UserParam, Long> {

    UserParam findById(UserParam.UserParamId id);

    void delete(UserParam userParam);
}
