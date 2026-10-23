.DEFAULT_GOAL := build
ANDROID_DEPENDENCIES := .build/bin/app/dependencies.gradle

$(ANDROID_DEPENDENCIES): resources/android.clj
	@ mkdir -p $(dir $@)
	@ ly2k --target eval < $< > $@

.PHONY: build
build: $(ANDROID_DEPENDENCIES)
	@ cat build.clj | ly2k --target eval | $(MAKE) -f - build

.PHONY: run
run: $(ANDROID_DEPENDENCIES)
	@ cat build.clj | ly2k --target eval | $(MAKE) -f - run

.PHONY: check
check: test
	@ git diff --check
	@ git diff --cached --check

.PHONY: test
test:
	@ mkdir -p .build/unit
	@ ly2k --target eval < build-test.clj > .build/unit/Makefile
	@ $(MAKE) -f .build/unit/Makefile build
	@ java .build/unit/src/argent/checks/runner.java
