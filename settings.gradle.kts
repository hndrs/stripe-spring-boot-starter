rootProject.name = "stripe-spring-boot-starter"

include("starter")
project(":starter").projectDir = File("starter")

include("sample")
project(":sample").projectDir = File("sample")
