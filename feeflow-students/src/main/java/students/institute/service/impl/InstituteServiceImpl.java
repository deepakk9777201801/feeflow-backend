package students.institute.service.impl;

import auth.entity.Institute;
import auth.entity.Role;
import auth.entity.User;
import auth.entity.UserRole;
import auth.repository.InstituteRepository;
import auth.repository.UserRepository;
import auth.repository.UserRoleRepository;
import common.dto.institute.CreateInstituteRequest;
import common.dto.institute.InstituteResponse;
import common.dto.institute.UpdateInstituteRequest;
import common.exception.BadRequestException;
import common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import students.institute.service.InstituteService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstituteServiceImpl implements InstituteService {

    private final InstituteRepository instituteRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public InstituteResponse createInstitute(CreateInstituteRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Creating institute '{}' by user: {}", request.getName(), email);

        if (instituteRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An institute with email '" + request.getEmail() + "' already exists");
        }

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Institute institute = Institute.builder()
                .name(request.getName())
                .city(request.getCity())
                .state(request.getState())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        Institute saved = instituteRepository.save(institute);

        UserRole adminRole = UserRole.builder()
                .user(currentUser)
                .institute(saved)
                .role(Role.INSTITUTE_ADMIN)
                .build();

        userRoleRepository.save(adminRole);
        log.info("Institute '{}' created with id: {}, user '{}' assigned as INSTITUTE_ADMIN", saved.getName(), saved.getId(), email);

        return toResponse(saved);
    }

    @Override
    public List<InstituteResponse> getMyInstitutes() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching institutes for user: {}", email);

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return instituteRepository.findDistinctInstitutesByUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "institutes", key = "#id")
    public InstituteResponse getInstituteById(Integer id) {
        log.debug("Fetching institute with id: {}", id);
        return instituteRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "institutes", key = "#id")
    public InstituteResponse updateInstitute(Integer id, UpdateInstituteRequest request) {
        log.info("Updating institute with id: {}", id);

        Institute institute = instituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found with id: " + id));

        Optional.ofNullable(request.getName()).ifPresent(institute::setName);
        Optional.ofNullable(request.getCity()).ifPresent(institute::setCity);
        Optional.ofNullable(request.getState()).ifPresent(institute::setState);
        Optional.ofNullable(request.getPhone()).ifPresent(institute::setPhone);
        Optional.ofNullable(request.getEmail()).ifPresent(institute::setEmail);

        Institute updated = instituteRepository.save(institute);
        log.info("Institute '{}' updated successfully", updated.getId());

        return toResponse(updated);
    }

    private InstituteResponse toResponse(Institute institute) {
        return InstituteResponse.builder()
                .id(institute.getId())
                .name(institute.getName())
                .city(institute.getCity())
                .state(institute.getState())
                .phone(institute.getPhone())
                .email(institute.getEmail())
                .createdAt(institute.getCreatedAt())
                .updatedAt(institute.getUpdatedAt())
                .build();
    }
}
