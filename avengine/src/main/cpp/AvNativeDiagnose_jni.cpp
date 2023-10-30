
#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include "AvNativeDiagnose_jni.h"
#include "ErrCode.h"
#include "AvDiagnoseEng.hpp"
#include "AvCodecUtility.hpp"
#include "JNIPublic.h"
#include "JNIHelper.hpp"


//
// 定义Android层的色彩格式
//

//
// Handler for media parser
//
typedef struct _AvCvterEngHandler {
    CAvDiagnoseEng*   pDiagnoseEng;

    JavaVM*       pJavaVM;
    jclass        JavaCtrlClass;
    jobject       JavaCtrlObj;

}AVDIAGNOSEERENG_HANDLER, *LPAVDIAGNOSEERENG_HANDLER;


//////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// JNI Interface Implementation ///////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
/*
 * Class:     io_agora_avmodule_AvNativeDiagnose
 * Method:    native_diagnoseOpen
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_io_agora_avmodule_AvNativeDiagnose_native_1diagnoseOpen (
    JNIEnv *env,
    jobject thiz,
    jstring jstr_src_url        )
 {
    auto srcFileUrl = getRAIIJString(env, jstr_src_url);
    if (srcFileUrl == nullptr) {
        LOGE("<native_1diagnoseOpen> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    std::string in_file_url = srcFileUrl.get();
    if (in_file_url.empty())  {
        LOGE("<native_1diagnoseOpen> [ERROR] invalid parameter\n");
        return XERR_INVALID_PARAM;
    }

    AVDIAGNOSEERENG_HANDLER * pEngHandler = nullptr;
    int res;

    pEngHandler = (AVDIAGNOSEERENG_HANDLER*)malloc(sizeof(AVDIAGNOSEERENG_HANDLER));
    assert(pEngHandler);
    if (nullptr == pEngHandler) {
        LOGE("<native_1diagnoseOpen> [ERROR] no memory\n");
        return 0;
    }
    memset(pEngHandler, 0, sizeof(AVDIAGNOSEERENG_HANDLER));

    env->GetJavaVM(&(pEngHandler->pJavaVM));
    if( NULL == pEngHandler->pJavaVM )
    {
        LOGE("<native_1diagnoseOpen> [ERROR] GetJavaVM failed\n");
        free(pEngHandler);
        return 0;
    }
    pEngHandler->JavaCtrlClass = (jclass)env->NewGlobalRef( env->GetObjectClass(thiz) );
    pEngHandler->JavaCtrlObj = env->NewGlobalRef(thiz);

    // 初始化诊断器
    pEngHandler->pDiagnoseEng = new CAvDiagnoseEng();
    int32_t ret = pEngHandler->pDiagnoseEng->Open(in_file_url);
    if (ret != XOK) {
        LOGE("<native_1diagnoseOpen> [ERROR] fail to open(), ret=%d\n", ret);
        free(pEngHandler);
        return 0;
    }

    LOGD("<native_1diagnoseOpen> done\n");
    return (jlong)(pEngHandler);
}


/*
 * Class:     io_agora_avmodule_AvNativeDiagnose
 * Method:    native_diagnoseClose
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvNativeDiagnose_native_1diagnoseClose
    (JNIEnv *env, jobject thiz, jlong jlEng)
{
    AVDIAGNOSEERENG_HANDLER* pEngHandler = (AVDIAGNOSEERENG_HANDLER*)jlEng;

    if (nullptr != pEngHandler)
    {
        if (pEngHandler->pDiagnoseEng)
        {
            pEngHandler->pDiagnoseEng->Close();
            delete pEngHandler->pDiagnoseEng;
            pEngHandler->pDiagnoseEng = nullptr;
        }

        if (pEngHandler->JavaCtrlObj)
        {
            env->DeleteGlobalRef(pEngHandler->JavaCtrlObj);
            pEngHandler->JavaCtrlObj = nullptr;
        }

        if (pEngHandler->JavaCtrlClass)
        {
            env->DeleteGlobalRef(pEngHandler->JavaCtrlClass);
            pEngHandler->JavaCtrlClass = nullptr;
        }

        free(pEngHandler);
        LOGD("<native_1diagnoseClose> done");
    }

    return XOK;
}

/*
 * Class:     io_agora_avmodule_AvNativeDiagnose
 * Method:    native_diagnoseGetMediaInfo
 * Signature: (JLio/agora/avmodule/AvMediaInfo;)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvNativeDiagnose_native_1diagnoseGetMediaInfo
  (JNIEnv *env, jobject thiz, jlong jlEng, jobject jobj_mediaInfo)
{
  AVDIAGNOSEERENG_HANDLER* pEngHandler = (AVDIAGNOSEERENG_HANDLER*)jlEng;

  if (nullptr == pEngHandler || nullptr == pEngHandler->pDiagnoseEng) {
    LOGE("<native_1diagnoseGetMediaInfo> [ERROR] invalid parameter\n");
    return XERR_INVALID_PARAM;
  }

  const AvMediaInfo* media_info_ptr = pEngHandler->pDiagnoseEng->GetMediaInfoPtr();
  if (media_info_ptr == NULL) {
    LOGE("<native_1diagnoseGetMediaInfo> [ERROR] bad state\n");
    return XERR_INVALID_PARAM;
  }


  //
  // 获取类对象的各个字段的 Field Id
  //
  jclass jclass_media_info = env->GetObjectClass(jobj_mediaInfo);
  jfieldID fid_file_path = env->GetFieldID(jclass_media_info, "mFilePath", kTypeString);
  jfieldID fid_duration = env->GetFieldID(jclass_media_info, "mFileDuration", kTypeLong);

  jfieldID fid_video_track = env->GetFieldID(jclass_media_info, "mVideoTrackId", kTypeInt);
  jfieldID fid_video_duration = env->GetFieldID(jclass_media_info, "mVideoDuration", kTypeLong);
  jfieldID fid_video_codec = env->GetFieldID(jclass_media_info, "mVideoCodec", kTypeString);
  jfieldID fid_color_format = env->GetFieldID(jclass_media_info, "mColorFormat", kTypeInt);
  jfieldID fid_color_range = env->GetFieldID(jclass_media_info, "mColorRange", kTypeInt);
  jfieldID fid_color_space = env->GetFieldID(jclass_media_info, "mColorSpace", kTypeInt);
  jfieldID fid_video_width = env->GetFieldID(jclass_media_info, "mVideoWidth", kTypeInt);
  jfieldID fid_video_height = env->GetFieldID(jclass_media_info, "mVideoHeight", kTypeInt);
  jfieldID fid_rotation = env->GetFieldID(jclass_media_info, "mRotation", kTypeInt);
  jfieldID fid_frame_rate = env->GetFieldID(jclass_media_info, "mFrameRate", kTypeInt);
  jfieldID fid_video_bitrate = env->GetFieldID(jclass_media_info, "mVideoBitrate", kTypeInt);
  jfieldID fid_video_max_bitrate = env->GetFieldID(jclass_media_info, "mVideoMaxBitrate", kTypeInt);

  jfieldID fid_audio_track = env->GetFieldID(jclass_media_info, "mAudioTrackId", kTypeInt);
  jfieldID fid_audio_duration = env->GetFieldID(jclass_media_info, "mAudioDuration", kTypeLong);
  jfieldID fid_audio_codec = env->GetFieldID(jclass_media_info, "mAudioCodec", kTypeString);
  jfieldID fid_sample_format = env->GetFieldID(jclass_media_info, "mSampleFmt", kTypeInt);
  jfieldID fid_channels = env->GetFieldID(jclass_media_info, "mChannels", kTypeInt);
  jfieldID fid_samle_rate = env->GetFieldID(jclass_media_info, "mSampleRate", kTypeInt);

  jfieldID fid_audio_bitrate = env->GetFieldID(jclass_media_info, "mAudioBitrate", kTypeInt);
  jfieldID fid_audio_max_bitrate = env->GetFieldID(jclass_media_info, "mAudioMaxBitrate", kTypeInt);

  //
  // 设置字段值
  //
  jstring  file_path = env->NewStringUTF((const char *)(media_info_ptr->file_path_.c_str()));
  env->SetObjectField(jobj_mediaInfo, fid_file_path, file_path);
  env->SetLongField(jobj_mediaInfo, fid_duration, (jlong)(media_info_ptr->file_duration_));

  env->SetIntField(jobj_mediaInfo, fid_video_track, (jint)(media_info_ptr->video_track_index_));
  if (media_info_ptr->video_track_index_ >= 0) {  // 有视频流信息
    env->SetLongField(jobj_mediaInfo, fid_video_duration,
                      (jlong) (media_info_ptr->video_duration_));
    std::string android_video_codec = "";
    CAvCodecUtility::MapAndroidCodec(media_info_ptr->video_codec_, android_video_codec);
    jstring video_codec = env->NewStringUTF((const char *) (android_video_codec.c_str()));
    env->SetObjectField(jobj_mediaInfo, fid_video_codec, video_codec);
    env->SetIntField(jobj_mediaInfo, fid_color_format, (jint) (media_info_ptr->color_format_));
    env->SetIntField(jobj_mediaInfo, fid_color_space, (jint) (media_info_ptr->color_space_));
    env->SetIntField(jobj_mediaInfo, fid_color_range, (jint) (media_info_ptr->color_range_));
    env->SetIntField(jobj_mediaInfo, fid_video_width, (jint) (media_info_ptr->video_width_));
    env->SetIntField(jobj_mediaInfo, fid_video_height, (jint) (media_info_ptr->video_height_));
    env->SetIntField(jobj_mediaInfo, fid_rotation, (jint) (media_info_ptr->rotation_));
    env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (media_info_ptr->frame_rate_));
    env->SetIntField(jobj_mediaInfo, fid_video_bitrate,
                     (jint) (media_info_ptr->video_bitrate_));
    env->SetIntField(jobj_mediaInfo, fid_video_max_bitrate,
                     (jint) (media_info_ptr->video_max_bitrate));
  }

  env->SetIntField(jobj_mediaInfo, fid_audio_track, (jint) (media_info_ptr->audio_track_index_));
  if (media_info_ptr->audio_track_index_ >= 0) {  // 有音频流信息
    env->SetLongField(jobj_mediaInfo, fid_audio_duration,
                      (jlong) (media_info_ptr->audio_duration_));
    std::string android_audio_codec = "";
    CAvCodecUtility::MapAndroidCodec(media_info_ptr->audio_codec_, android_audio_codec);
    jstring audio_codec = env->NewStringUTF((const char *) (android_audio_codec.c_str()));
    env->SetObjectField(jobj_mediaInfo, fid_audio_codec, audio_codec);

    int32_t android_format = ENCODING_PCM_16BIT;
    CAvCodecUtility::MapAndroidSampleFormat(media_info_ptr->sample_foramt_, android_format);
    env->SetIntField(jobj_mediaInfo, fid_sample_format, (jint) (android_format));
    env->SetIntField(jobj_mediaInfo, fid_channels, (jint) (media_info_ptr->channels_));
    env->SetIntField(jobj_mediaInfo, fid_samle_rate, (jint) (media_info_ptr->sample_rate_));
    env->SetIntField(jobj_mediaInfo, fid_frame_rate, (jint) (media_info_ptr->frame_rate_));
    env->SetIntField(jobj_mediaInfo, fid_audio_bitrate,
                     (jint) (media_info_ptr->audio_bitrate_));
    env->SetIntField(jobj_mediaInfo, fid_audio_max_bitrate,
                     (jint) (media_info_ptr->audio_max_bitrate));
  }

  return XOK;
}

/*
 * Class:     io_agora_avmodule_AvNativeDiagnose
 * Method:    native_diagnoseDoStep
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvNativeDiagnose_native_1diagnoseDoStep
  (JNIEnv *env, jobject thiz, jlong jlEng)
{
  AVDIAGNOSEERENG_HANDLER* pEngHandler = (AVDIAGNOSEERENG_HANDLER*)jlEng;

  if (nullptr == pEngHandler || nullptr == pEngHandler->pDiagnoseEng) {
    LOGE("<native_1diagnoseDoStep> [ERROR] invalid parameter\n");
    return XERR_INVALID_PARAM;
  }

  int ret = pEngHandler->pDiagnoseEng->DoPrasing();
  return (jint)ret;
}

/*
 * Class:     io_agora_avmodule_AvNativeDiagnose
 * Method:    native_diagnoseGetProgress
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_io_agora_avmodule_AvNativeDiagnose_native_1diagnoseGetProgress
  (JNIEnv *env, jobject thiz, jlong jlEng)
{
  AVDIAGNOSEERENG_HANDLER* pEngHandler = (AVDIAGNOSEERENG_HANDLER*)jlEng;

  if (nullptr == pEngHandler || nullptr == pEngHandler->pDiagnoseEng) {
    LOGE("<native_1diagnoseGetProgress> [ERROR] invalid parameter\n");
    return 0;
  }

  int progress = pEngHandler->pDiagnoseEng->GetParseProgress();
  return (jint)progress;
}