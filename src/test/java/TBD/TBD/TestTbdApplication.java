package TBD.TBD;

import org.springframework.boot.SpringApplication;

public class TestTbdApplication {

	public static void main(String[] args) {
		SpringApplication.from(TbdApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
