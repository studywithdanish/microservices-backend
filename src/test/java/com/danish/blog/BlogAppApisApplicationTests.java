package com.danish.blog;

import com.danish.blog.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BlogAppApisApplicationTests {

    @Autowired
    private UserRepo userRepo;

	@Test
	void contextLoads() {
	}

    @Test
    public void repoTest(){
        String className = this.userRepo.getClass().getName();
        String packageName = this.userRepo.getClass().getPackageName();
        System.out.println("Class Name:" +className);
        System.out.println("Package Name:" +packageName);
    }

}
