FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Avoid prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Install base dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    git \
    curl \
    wget \
    unzip \
    zip \
    lib32stdc++6 \
    lib32z1 \
    qemu-kvm \
    libvirt-daemon-system \
    libvirt-clients \
    bridge-utils \
    cpu-checker \
    adb \
    && rm -rf /var/lib/apt/lists/*

# Set Java 17 as default
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Android SDK command-line tools
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && \
    mv cmdline-tools latest && \
    rm cmdline-tools.zip

# Accept Android SDK licenses
RUN yes | sdkmanager --licenses > /dev/null 2>&1

# Install Android SDK components
RUN sdkmanager --install \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "emulator" \
    "system-images;android-34;google_apis;arm64-v8a" \
    "extras;android;m2repository" \
    "extras;google;m2repository" \
    "extras;google;google_play_services"

# Set up Android SDK permissions
RUN chmod -R 755 ${ANDROID_HOME}

# Create workspace directory
RUN mkdir -p /workspace && \
    chown -R vscode:vscode /workspace

# Create cache directories
RUN mkdir -p /home/vscode/.gradle && \
    mkdir -p /home/vscode/.android && \
    chown -R vscode:vscode /home/vscode

# Install Gradle
ENV GRADLE_VERSION=8.5
RUN cd /tmp && \
    wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip -q gradle-${GRADLE_VERSION}-bin.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle && \
    rm gradle-${GRADLE_VERSION}-bin.zip

# Install Kotlin
ENV KOTLIN_VERSION=1.9.22
RUN cd /tmp && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip && \
    unzip -q kotlin-compiler-${KOTLIN_VERSION}.zip -d /opt && \
    ln -s /opt/kotlinc/bin/kotlinc /usr/local/bin/kotlinc && \
    rm kotlin-compiler-${KOTLIN_VERSION}.zip

# Copy setup scripts
COPY setup.sh /tmp/setup.sh
COPY start-emulator.sh /tmp/start-emulator.sh
COPY run-tests.sh /tmp/run-tests.sh
RUN chmod +x /tmp/*.sh

# Switch to vscode user
USER vscode

# Set working directory
WORKDIR /workspace
