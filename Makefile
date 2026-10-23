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
	@ python3 test/logging.py
	@ cat src/argent/vendor/effect.clj src/argent/vendor/event.clj test/registration.eval | ly2k --target eval
	@ cat src/argent/vendor/qr_image.clj test/qr_image.eval | ly2k --target eval
	@ cat src/argent/vendor/effect.clj src/argent/vendor/event.clj src/argent/main.clj test/qr_example.eval | ly2k --target eval
	@ cat src/argent/vendor/effect.clj src/argent/vendor/event.clj src/argent/vendor/view_node.clj test/qr_button.eval | ly2k --target eval
	@ cat src/argent/vendor/effect.clj src/argent/vendor/event.clj src/argent/main.clj src/argent/application.clj test/argent/counter.clj test/counter.eval | ly2k --target eval
	@ cat src/argent/vendor/effect.clj src/argent/vendor/event.clj src/argent/main.clj src/argent/application.clj src/argent/vendor/event_pump.clj test/event_pump.eval | ly2k --target eval
