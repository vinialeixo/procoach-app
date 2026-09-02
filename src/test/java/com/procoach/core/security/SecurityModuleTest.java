package com.procoach.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.procoach.core.security.application.RoleService;
import com.procoach.core.security.application.UserService;
import com.procoach.core.security.domain.Permission;
import com.procoach.core.security.domain.Role;
import com.procoach.core.security.domain.User;

@SpringBootTest(classes = ProcoachApplication.class)
@Import(TestcontainersConfiguration.class)
class SecurityModuleTest {

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	@Test
	void createsRoleAndUserWithRoleReference() {
		Role coach = roleService.createRole("COACH", Set.of(Permission.MANAGE_ATHLETES, Permission.MANAGE_PROTOCOLS));

		User user = userService.createUser("coach1", "coach1@example.com", "hash", Set.of(coach.getId()));

		assertThat(user.getId()).isNotNull();
		assertThat(user.getRoleIds()).containsExactly(coach.getId());
		assertThat(user.isEnabled()).isTrue();

		User loaded = userService.findById(user.getId()).orElseThrow();
		assertThat(loaded.getUsername()).isEqualTo("coach1");
		assertThat(loaded.getRoleIds()).containsExactly(coach.getId());
	}

	@Test
	void rejectsUserCreationWithUnknownRole() {
		assertThatThrownBy(() -> userService.createUser("x", "x@example.com", "hash", Set.of(UUID.randomUUID())))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void assignsRoleToExistingUser() {
		Role athlete = roleService.createRole("ATHLETE", Set.of(Permission.VIEW_OWN_DATA));
		User user = userService.createUser("athlete1", "athlete1@example.com", "hash", Set.of());

		User updated = userService.assignRole(user.getId(), athlete.getId());

		assertThat(updated.getRoleIds()).containsExactly(athlete.getId());
	}
}
