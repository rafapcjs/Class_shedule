package com.unicar.Class_shedule.commons.students.services.implementation;

import com.unicar.Class_shedule.commons.security.persistencie.entities.RoleEntity;
import com.unicar.Class_shedule.commons.security.persistencie.entities.RoleEnum;
import com.unicar.Class_shedule.commons.security.persistencie.entities.UserEntity;
import com.unicar.Class_shedule.commons.security.persistencie.repositories.RoleRepository;
import com.unicar.Class_shedule.commons.students.factory.StudentFactory;
import com.unicar.Class_shedule.commons.students.persistencie.entity.Student;
import com.unicar.Class_shedule.commons.students.persistencie.repositories.IStudentsRepository;
import com.unicar.Class_shedule.commons.students.presentation.dto.StudentDto;
import com.unicar.Class_shedule.commons.students.presentation.payload.StudentPayload;
import com.unicar.Class_shedule.commons.students.services.interfaces.IStudentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements IStudentService {

    private  final IStudentsRepository  iStudentsRepository;
    private  final StudentFactory studentFactory;
    private final PasswordEncoder passwordEncoder;
    private  final RoleRepository roleRepository;

    @Override
    @Transactional
    public void saveStudent(StudentPayload studentPayload) {

        List<RoleEntity>roleEntities =roleRepository.findByRoleEnum(RoleEnum.STUDENT);
        if (roleEntities.isEmpty()){

            throw new IllegalArgumentException("role student not found");

        }

        RoleEntity roleEntity = roleEntities.stream()
                .filter(role -> role.getRoleEnum() == RoleEnum.STUDENT)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ROLE STUDENT NOT FOUND IN THE FILTER"));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleEntity);


        // Crear una instancia de UserEntity a partir de los datos del payload
        UserEntity userEntity = UserEntity.builder()
                .username(studentPayload.getUsername())
                .fullName(studentPayload.getFullName())
                .dni(studentPayload.getDni())
                .phoneNumber(studentPayload.getPhoneNumber())
                .address(studentPayload.getAddress())
                .email(studentPayload.getEmail())
                .password(studentPayload.getPassword())
                .isEnabled(true)
                .accountNoLocked(true)
                .accountNoExpired(true)
                .credentialNoExpired(true)
                .build();
   Student student = Student.builder()

           .carrer(studentPayload.getCarrer())
           .description(studentPayload.getDescription())
           .userEntity(userEntity)
           .build();

   iStudentsRepository.save(student);

    }
    @Override
    @Transactional
    public void deleteByDni(String dni) {
        Student student = iStudentsRepository.findByUserEntityDni(dni)
                .orElseThrow(() -> new EntityNotFoundException("STUDENT NOT FOUND WITH DNI " + dni));
        iStudentsRepository.delete(student);
    }

    @Override
    @Transactional
    public void updateStudent(StudentPayload studentPayload, String dni) {
        Student student = iStudentsRepository.findByUserEntityDni(dni)
                .orElseThrow(() -> new EntityNotFoundException("STUDENT NOT FOUND WITH DNI " + dni));

        UserEntity userEntity = student.getUserEntity();
        userEntity.setUsername(studentPayload.getUsername());
        userEntity.setFullName(studentPayload.getFullName());
        userEntity.setPhoneNumber(studentPayload.getPhoneNumber());
        userEntity.setAddress(studentPayload.getAddress());
        userEntity.setEmail(studentPayload.getEmail());

        // Update password only if it's not null or empty
        if (studentPayload.getPassword() != null && !studentPayload.getPassword().isEmpty()) {
            userEntity.setPassword(passwordEncoder.encode(studentPayload.getPassword())); // Hash the password
        }


        student.setCarrer(studentPayload.getCarrer());
        student.setDescription(studentPayload.getDescription());
        student.setUserEntity(userEntity);
        iStudentsRepository.save(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentDto> findStudentByDni(String dni) {
        return iStudentsRepository.findByUserEntityDni(dni)
                .map(student -> studentFactory.studentDto(student))
                .map(Optional::of)
                .orElseThrow(() -> new EntityNotFoundException("STUDENT NOT FOUND WITH DNI " + dni));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDto> findStudents(Pageable pageable) {

        Page<Student>studentPage =iStudentsRepository.findAll(pageable);
        List<StudentDto>studentDtoList=studentPage.stream().map(
                student -> studentFactory.studentDto(student)
        ).collect(Collectors.toList());
        return new PageImpl<>(studentDtoList, pageable,studentPage.getTotalElements());
    }


}
























