package com.danish.blog;

import com.danish.blog.entities.Role;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.repositories.RoleRepo;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class BlogAppApisApplication implements CommandLineRunner {

    private final RoleRepo roleRepo;

    public BlogAppApisApplication(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    public static void main(String[] args) {

        SpringApplication.run(BlogAppApisApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }


    @Override
    public void run(String... args) throws Exception {
        if (roleRepo.count() == 0) {
            Role role1 = new Role();
            role1.setId(AppConstants.ADMIN_USER);
            role1.setName("ROLE_ADMIN");

            Role role2 = new Role();
            role2.setId(AppConstants.NORMAL_USER);
            role2.setName("ROLE_NORMAL");

            List<Role> roles = List.of(role1, role2);
            roleRepo.saveAll(roles);
        }
    }
}
