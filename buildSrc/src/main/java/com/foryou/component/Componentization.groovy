class ComponentCoding implements Plugin<Project> {

    @Override
    void apply(Project target) {
        def propertyFile = target.file("gradle.properties")

        if (!propertyFile.exists()) {
            println 'no exist'
            propertyFile.parentFile.mkdir()
            propertyFile.text = "isDependent=false\ncompileProject="//暂时先添加project
            println propertyFile.text
            if (!target.rootProject.hasProperty("mainModuleName")) {
                target.rootProject.setProperty("mainModuleName=app")
            }
        }

        def isDependent = Boolean.parseBoolean(target.properties.get("isDependent"))//从外部文件读取，例如properties文件
        println isDependent.toString()

        def mainModuleName = target.rootProject.property("mainModule")
        println mainModuleName

        if (target.getName() == mainModuleName) {
            isDependent = true
            target.setProperty("isDependent", true)
        }

        if (isDependent && isAssemble(target.gradle.startParameter.getTaskNames())) {
            target.apply plugin: 'com.android.application'

            target.android.sourceSets {
                main {
                    manifest.srcFile 'src/main/AndroidManifest.xml'
                    java.srcDirs = ['src/main/java', 'src/main/debug/java']
                    res.srcDirs = ['src/main/res', 'src/main/debug/res']
                    java {
                        exclude 'debug/**'
                    }
                }
            }
            compileDependentProject(target)
            initIApplication(target)
        } else {
            target.apply plugin: 'com.android.library'
//            target.android.resourcePrefix "$project.getName()_"
        }
    }

    /**
     * 将依赖组件动态引入，强制解耦合
     * @param project
     */
    static void compileDependentProject(Project project) {
        String allProject = project.properties.get("compileProject")
        if (allProject == null && allProject.length() == 0) {
            return
        }
        def compileProjects = allProject.split(",")
        if (compileProjects == null || compileProjects.length == 0) {
            return
        }
        def mainModuleName = project.rootProject.property("mainModule")
        compileProjects.each {
            if (project.getName() != mainModuleName) {
                project.dependencies.add("implementation", project.project(":$it"))
            }
        }
    }

    static def initIApplication(Project project) {
        //todo 实现application初始化代码时初始化组件代码，解除初始化代码耦合
    }

    static boolean isAssemble(List<String> taskNames) {
        def isAssemble = false
        taskNames.each {
            if (it.toUpperCase().contains("ASSEMBLE")
                    || it.contains("aR")
                    || it.contains("asR")
                    || it.contains("asD")
                    || it.toUpperCase().contains("INSTALL")
            ) {
                isAssemble = true
            }
        }
        isAssemble
    }
}
