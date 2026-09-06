package com.edafa.sms_submitter.repository;
import com.edafa.sms_submitter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
    boolean existsUserByFirstNameAndLastName(String firstName, String secondName);
    User findUserByFirstNameAndLastNameAndCompanyCompanyId(String firstName, String secondName, Integer companyId);
    boolean existsByPhoneNo(String phoneNo);
    boolean existsUserByPhoneNoAndCompanyCompanyId(String phoneNo, Integer companyId);
    boolean existsUserByPhoneNoAndCompanyCompanyIdAndUserIdNot(String phoneNo, Integer companyId, Integer userId);

    User findUserByPhoneNoAndCompanyCompanyId(String phoneNo, Integer companyId);
    Optional<User> findByPhoneNo(String phoneNo);
    List<User> findAllByCompanyCompanyId(Integer companyId);
}
