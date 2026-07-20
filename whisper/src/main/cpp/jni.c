#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define TAG "whisper_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

JNIEXPORT jlong JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_initContext(
        JNIEnv *env, jclass clazz, jstring model_path) {
    (void) clazz;
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    if (ctx == NULL) {
        LOGE("failed to load model from %s", path);
    } else {
        LOGI("loaded model from %s", path);
    }
    (*env)->ReleaseStringUTFChars(env, model_path, path);
    return (jlong) (intptr_t) ctx;
}

JNIEXPORT void JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_freeContext(
        JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) env;
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    if (ctx != NULL) {
        whisper_free(ctx);
    }
}

JNIEXPORT jint JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_fullTranscribe(
        JNIEnv *env, jclass clazz, jlong ctx_ptr, jint n_threads,
        jstring lang, jboolean translate, jfloatArray pcm) {
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    if (ctx == NULL) return -100;

    // Language string must stay valid for the whole whisper_full call.
    static char lang_buf[16];
    const char *lang_chars = (*env)->GetStringUTFChars(env, lang, NULL);
    strncpy(lang_buf, lang_chars, sizeof(lang_buf) - 1);
    lang_buf[sizeof(lang_buf) - 1] = '\0';
    (*env)->ReleaseStringUTFChars(env, lang, lang_chars);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = lang_buf[0] != '\0' ? lang_buf : "auto";
    params.translate = translate;
    params.n_threads = n_threads > 0 ? n_threads : 4;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;

    jfloat *samples = (*env)->GetFloatArrayElements(env, pcm, NULL);
    jsize n_samples = (*env)->GetArrayLength(env, pcm);
    LOGI("whisper_full: %d samples, lang=%s, translate=%d, threads=%d",
         (int) n_samples, params.language, (int) translate, params.n_threads);

    int ret = whisper_full(ctx, params, samples, n_samples);
    (*env)->ReleaseFloatArrayElements(env, pcm, samples, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_getSegmentCount(
        JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) env;
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    return whisper_full_n_segments(ctx);
}

JNIEXPORT jstring JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_getSegmentText(
        JNIEnv *env, jclass clazz, jlong ctx_ptr, jint index) {
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    const char *text = whisper_full_get_segment_text(ctx, index);
    return (*env)->NewStringUTF(env, text);
}

/* Segment times are in 10 ms ticks (whisper convention). */
JNIEXPORT jlong JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_getSegmentT0(
        JNIEnv *env, jclass clazz, jlong ctx_ptr, jint index) {
    (void) env;
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    return whisper_full_get_segment_t0(ctx, index);
}

JNIEXPORT jlong JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_getSegmentT1(
        JNIEnv *env, jclass clazz, jlong ctx_ptr, jint index) {
    (void) env;
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    return whisper_full_get_segment_t1(ctx, index);
}

JNIEXPORT jstring JNICALL
Java_com_seekersofjustice_whisper_WhisperLib_detectedLanguage(
        JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) ctx_ptr;
    int lang_id = whisper_full_lang_id(ctx);
    const char *lang = lang_id >= 0 ? whisper_lang_str(lang_id) : "en";
    return (*env)->NewStringUTF(env, lang);
}
