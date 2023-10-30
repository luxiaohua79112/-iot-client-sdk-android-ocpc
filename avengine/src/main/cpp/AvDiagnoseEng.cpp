/**
 * @file CAvDiagnoseEng.cpp
 * @brief This file implement the convert engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-09-26
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#include "comtypedef.hpp"
#include "AvDiagnoseEng.hpp"

//
// 用于解码输出的配置
//
#define OUT_FRAME_FORAMT                AV_PIX_FMT_NV12             // 视频帧解码为NV12
#define OUT_SAMPLE_FMT                  AV_SAMPLE_FMT_S16           // 输出音频采样格式
#define OUT_CHANNELS                    2                           // 输出双频道
#define OUT_SAMPLE_RATE                 48000                       // 输出采样率



//
// FFMPEG内部日志输出
//
static void DiagnoseLogCallback(void *ptr, int level, const char *fmt, va_list vl)
{
    va_list vl2;
    char line[1024];
    static int print_prefix = 1;

    va_copy(vl2, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, sizeof(line), &print_prefix);
    va_end(vl2);

    LOGD("%s", line);
}

///////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// Public Methods ////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////
CAvDiagnoseEng::CAvDiagnoseEng()
{
    /* register all codecs, demux and protocols */
    avcodec_register_all();
#if CONFIG_AVDEVICE
    avdevice_register_all();
#endif
#if CONFIG_AVFILTER
    avfilter_register_all();
#endif
    av_register_all();
    avformat_network_init();

    av_log_set_callback(DiagnoseLogCallback);
}

CAvDiagnoseEng::~CAvDiagnoseEng()
{
    avformat_network_deinit();
}



int32_t CAvDiagnoseEng::Open(std::string src_file_path)
{
    int32_t ret;
    int32_t i;

    src_file_path_ = src_file_path;

    ret = InStreamOpen();
    if (ret != XOK) {
        return ret;
    }

    LOGD("<CAvDiagnoseEng::Open> done, ret=%d\n", ret);
    return ret;
}

int32_t CAvDiagnoseEng::Close()
{
    InStreamClose();

    LOGD("<CAvDiagnoseEng::Close> done");
    return XOK;
}

const AvMediaInfo* CAvDiagnoseEng::GetMediaInfoPtr() {
    return (in_media_info_.get());
}

int32_t CAvDiagnoseEng::DoPrasing()  {
  int ret;

  // 连续送入数据包
  if (!input_eos_) {
    int32_t pkt_type = 0;
    ret = InputPacket(pkt_type);
    if (ret == XERR_CODEC_DEC_EOS) {  // 视频帧已经送入完成
      LOGD("<CAvDiagnoseEng::DoPrasing> feeding EOS done!");
      input_eos_ = true;

    } else if (ret != XOK) {
      LOGE("<CAvDiagnoseEng::DoPrasing> feeding packet error, ret=%d!", ret);
    }
  }

  //
  // 解码视频帧，如果可以解码成功则反复进行操作
  // 直到返回 错误 或者 XERR_CODEC_DEC_EOS 或者 XERR_CODEC_INDATA
  //
  while (!video_dec_eos_)
  {
    ret = DecodeVideoFrame();

    if (ret == XERR_CODEC_DEC_EOS) {  // 视频帧解码完成
      LOGD("<CAvDiagnoseEng::DoPrasing> video decoding EOS done!");
      video_dec_eos_ = true;
    }

    if (ret != XOK) {
      break;
    }
  }

  //
  // 解码音频帧，如果可以解码成功则反复进行操作
  // 直到返回 错误 或者 XERR_CODEC_DEC_EOS 或者 XERR_CODEC_INDATA
  //
  while (!audio_dec_eos_)
  {
    ret = DecodeAudioFrame();
    if (ret == XERR_CODEC_DEC_EOS) {  // 音频帧解码完成
      LOGD("<CAvDiagnoseEng::DoPrasing> audio decoding EOS done!");
      audio_dec_eos_ = true;
    }

    if (ret != XOK) {
      break;
    }
  }

  if (video_dec_eos_ && audio_dec_eos_) {
    LOGD("<CAvDiagnoseEng::DoPrasing> total parsing all done!\n");
    return XERR_FILE_EOF;
  }

  return XOK;
}

int32_t CAvDiagnoseEng::GetParseProgress() {
    return parse_progress_;
}



int32_t CAvDiagnoseEng::InputPacket(int32_t& pkt_type)
{
  int32_t ret;
  pkt_type = 0;

  //
  // 读取一个数据包
  //
  AVPacketPtr packet = AVPacketPtrCreate();
  ret = av_read_frame(in_format_ctx_.get(), packet.get());
  if (ret == AVERROR_EOF) {   // 媒体文件读取结束
    video_progress_.SetInputEos();   // 标记最有一帧音视频帧的时间戳
    audio_progress_.SetInputEos();
    LOGE("<CAvDiagnoseEng::InputPacket> av_read_frame() EOF, lastVidPst=%"
           PRId64 ", lastAudPts=%" PRId64 "\n",
         video_progress_.GetLastPtkPts(), audio_progress_.GetLastPtkPts() );
    return XERR_CODEC_DEC_EOS;

  } else if (ret < 0) {   // 数据包读取失败
    LOGE("<CAvDiagnoseEng::InputPacket> [ERROR] fail to av_read_frame(), ret=%d\n", ret);
  }


  if (packet->stream_index == in_media_info_->video_track_index_) {    // 视频
    // 打印视频包信息
    int64_t video_pts = packet->pts - in_video_stream_->start_time;
    int64_t video_time = static_cast<int64_t>(video_pts * 1000 * av_q2d(in_video_stream_->time_base) * 1000);
    float video_time_sec = (float)(video_time / 1000000.0f);

    LOGD("<CAvDiagnoseEng::InputPacket> [VIDEO_PKT] pkt_pts=%" PRId64 ", pkt_dts=%" PRId64 ", pkt_size=%d"
           ", video_time=%" PRId64 ", video_time_sec=%f, pkt_flags=%d\n",
         packet->pts, packet->dts, packet->size, video_time, video_time_sec, packet->flags);

  } else if (packet->stream_index == in_media_info_->audio_track_index_) { // 音频
    // 打印音频包信息
    int64_t audio_pts = packet->pts - in_audio_stream_->start_time;
    int64_t audio_time = static_cast<int64_t>(audio_pts * 1000 * av_q2d(in_audio_stream_->time_base) * 1000);
    float audio_time_sec = (float)(audio_time / 1000000.0f);

    LOGD("<CAvDiagnoseEng::InputPacket> [AUDIO_PKT] pkt_pts=%" PRId64 ", pkt_dts=%" PRId64 ", pkt_size=%d"
           ", audio_time=%" PRId64 ", audio_time_sec=%f, pkt_flags=%d\n",
         packet->pts, packet->dts, packet->size, audio_time, audio_time_sec, packet->flags);
  }


  //
  // 将数据包送入相应的解码器
  //
  if (packet->stream_index == in_media_info_->video_track_index_) {
    video_progress_.SetInputPts(packet->pts);   // 设置当前视频包时间戳
    pkt_type = 1;
    ret = avcodec_send_packet(video_codec_ctx_.get(), packet.get());    // 送入视频解码器
  } else  if (packet->stream_index == in_media_info_->audio_track_index_) {
    audio_progress_.SetInputPts(packet->pts);   // 设置当前音频包时间戳
    pkt_type = 2;
    ret = avcodec_send_packet(audio_codec_ctx_.get(), packet.get());    // 送入音频解码器
  }
  if (ret == AVERROR(EAGAIN))  { // 没有数据送入,但是可以继续可以从内部缓冲区读取编码后的视频包
    LOGD("<CAvDiagnoseEng::InputPacket> avcodec_send_frame() EAGAIN\n");

  } else if (ret == AVERROR_EOF) {  // 数据包送入结束不再送入,但是可以继续可以从内部缓冲区读取编码后的视频包
    LOGE("<CAvDiagnoseEng::InputPacket> avcodec_send_frame() EOF\n");
    ret = XERR_CODEC_DEC_EOS;

  } else if (ret < 0) { // 送入输入数据包失败
    LOGE("<CAvDiagnoseEng::InputPacket> [ERROR] fail to avcodec_send_frame(), ret=%d\n", ret);
  }

  LOGD("<CAvDiagnoseEng::InputPacket> done\n");
  packet.reset();
  return ret;
}


int32_t CAvDiagnoseEng::DecodeVideoFrame()
{
  int ret;

  if (video_codec_ctx_ == nullptr) {
    return XERR_BAD_STATE;
  }


  AVFramePtr decoded_frame = AVFramePtrCreate();
  ret = avcodec_receive_frame(video_codec_ctx_.get(), decoded_frame.get());
  if (ret == AVERROR(EAGAIN)) // 当前这次没有解码后的音视频帧输出,需要 avcodec_send_packet()送入更多的数据
  {
    //LOGD("<CAvDiagnoseEng::DecodeVideoFrame> no data output\n");
    decoded_frame.reset();
    video_progress_.ResetDecodeCount();
    if (video_progress_.IsDecodeEos())  { // 视频解码完成
      LOGD("<CAvDiagnoseEng::DecodeVideoFrame> video decoding EOS\n");
      return XERR_CODEC_DEC_EOS;
    }
    return XERR_CODEC_INDATA;

  }
  else if (ret == AVERROR_EOF) // 解码缓冲区已经刷新完成,后续不再有数据输出
  {
    LOGD("<CAvDiagnoseEng::DecodeVideoFrame> decoder is EOF\n");
    decoded_frame.reset();
    return XERR_CODEC_DEC_EOS;

  } else if (ret < 0) {
    LOGE("<CAvDiagnoseEng::DecodeVideoFrame> [ERROR] fail to avcodec_receive_packet(), ret=%d\n", ret);
    decoded_frame.reset();
    video_progress_.IncreaseDecodeCount();
    if (video_progress_.IsDecodeEos())  { // 视频解码完成
      LOGD("<CAvDiagnoseEng::DecodeVideoFrame> video decoding EOS\n");
      return XERR_CODEC_DEC_EOS;
    }
    return XERR_CODEC_DECODING;
  }
  video_progress_.ResetDecodeCount();
  video_progress_.SetDecodedPts(decoded_frame->pts);


  LOGD("<CAvDiagnoseEng::DecodeVideoFrame> [VIDEO_FRAME], format=%d, w=%d, h=%d, pts=%" PRId64
        ", flags=%d, key_frame=%d\n",
       decoded_frame->format, decoded_frame->width, decoded_frame->height, decoded_frame->pts,
       decoded_frame->flags, decoded_frame->key_frame);
  decoded_frame.reset();

  if (video_progress_.IsDecodeEos()) { // 视频解码完成
    LOGD("<CAvDiagnoseEng::DecodeVideoFrame> video decoding EOS\n");
    return XERR_CODEC_DEC_EOS;
  }

  return XOK;
}


int32_t CAvDiagnoseEng::DecodeAudioFrame()
{
  int ret;

  if (audio_codec_ctx_ == nullptr) {
    return XERR_BAD_STATE;
  }


  AVFramePtr decoded_frame = AVFramePtrCreate();
  ret = avcodec_receive_frame(audio_codec_ctx_.get(), decoded_frame.get());
  if (ret == AVERROR(EAGAIN)) // 当前这次没有解码后的音视频帧输出,需要 avcodec_send_packet()送入更多的数据
  {
    //LOGD("<CAvDiagnoseEng::DecodeAudioFrame> no data output\n");
    decoded_frame.reset();
    audio_progress_.ResetDecodeCount();
    if (audio_progress_.IsDecodeEos())  { // 音频解码完成
      LOGD("<CAvDiagnoseEng::DecodeAudioFrame> audio decoding EOS\n");
      return XERR_CODEC_DEC_EOS;
    }
    return XERR_CODEC_INDATA;

  } else if (ret == AVERROR_EOF) // 解码缓冲区已经刷新完成,后续不再有数据输出
  {
    LOGD("<CAvDiagnoseEng::DecodeAudioFrame> decoder is EOF\n");
    decoded_frame.reset();
    return XERR_CODEC_DEC_EOS;

  } else if (ret < 0) {
    LOGE("<CAvDiagnoseEng::DecodeAudioFrame> [ERROR] fail to avcodec_receive_packet(), ret=%d\n", ret);
    audio_progress_.IncreaseDecodeCount();
    if (audio_progress_.IsDecodeEos())  { // 音频解码完成
      LOGD("<CAvDiagnoseEng::DecodeAudioFrame> audio decoding EOS\n");
      return XERR_CODEC_DEC_EOS;
    }
    return XERR_CODEC_DECODING;
  }
  audio_progress_.ResetDecodeCount();
  audio_progress_.SetDecodedPts(decoded_frame->pts);


  int frame_duration = 0;
  if (decoded_frame->sample_rate > 0) {
    frame_duration = (decoded_frame->nb_samples * 1000 / decoded_frame->sample_rate);
  }

  LOGD("<CAvDiagnoseEng::DecodeAudioFrame> [AUDIO_FRAME], format=%d, smpl_rate=%d, nb_samples=%d, pts=%" PRId64
         ", frame_duration=%d, flags=%d, key_frame=%d\n",
       decoded_frame->format, decoded_frame->sample_rate, decoded_frame->nb_samples, decoded_frame->pts,
       frame_duration, decoded_frame->flags, decoded_frame->key_frame);
  decoded_frame.reset();

  if (audio_progress_.IsDecodeEos()) { // 音频解码完成
    LOGD("<CAvDiagnoseEng::DecodeAudioFrame> audio decoding EOS\n");
    return XERR_CODEC_DEC_EOS;
  }

  return XOK;
}



///////////////////////////////////////////////////////////////////////////////
/////////////////////// Internal Methods for Input Stream /////////////////////
///////////////////////////////////////////////////////////////////////////////
int32_t CAvDiagnoseEng::InStreamOpen() {
    int ret, i;

    in_media_info_ = MakeUniquePtr<AvMediaInfo>();

    // 打开媒体文件
    in_format_ctx_ = AVFormatOpenContextPtrCreate(src_file_path_.c_str());
    if (in_format_ctx_ == nullptr)  {
        LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] in_format_ctx_ is NULL\n");
        return XERR_FILE_OPEN;
    }

    // 查询所有的媒体流
    ret = avformat_find_stream_info(in_format_ctx_.get(), nullptr);
    if (ret < 0)   {
        LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] fail to avformat_find_stream_info(), ret=%d", ret);
        return XERR_FILE_NO_STREAM;
    }

    // 遍历媒体流信息
    in_video_stream_ = NULL;
    in_audio_stream_ = NULL;
    in_video_codec_ = NULL;
    in_audio_codec_ = NULL;
    for (i = 0; i < in_format_ctx_->nb_streams; i++)
    {
        AVStream *pAvStream = in_format_ctx_->streams[i];
        if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            if ((pAvStream->codecpar->width <= 0) || (pAvStream->codecpar->height <= 0))
            {
                LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] invalid resolution, streamIndex=%d\n", i);
                continue;
            }
            in_video_stream_ = pAvStream;

            // 视频解码器
            in_video_codec_ = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (in_video_codec_ == nullptr)
            {
                LOGE("<CAvDiagnoseEng::Open> [ERROR] can not find video codecId=%d\n",
                     pAvStream->codecpar->codec_id);
                continue;
            }

            // 解析视频流信息
            in_media_info_->video_track_index_ = i;
            in_media_info_->video_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            in_media_info_->video_duration_ =
              static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            in_media_info_->color_format_ = pAvStream->codecpar->format;
            in_media_info_->color_range_ = pAvStream->codecpar->color_range;
            in_media_info_->color_space_ = pAvStream->codecpar->color_space;
            in_media_info_->video_width_  = pAvStream->codecpar->width;
            in_media_info_->video_height_ = pAvStream->codecpar->height;
            in_media_info_->rotation_ = ParseRotateAngle(pAvStream);
            if ((pAvStream->r_frame_rate.num > 0) && (pAvStream->r_frame_rate.den > 0)) {
                in_media_info_->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            } else if ((pAvStream->avg_frame_rate.num > 0) && (pAvStream->avg_frame_rate.den > 0)) {
                in_media_info_->frame_rate_ = static_cast<int32_t>((float)(pAvStream->r_frame_rate.num) / (float)(pAvStream->r_frame_rate.den) + 0.5f);
            } else {
                in_media_info_->frame_rate_ = 30;
            }

            if ((in_media_info_->video_duration_ <= 0) && (in_format_ctx_->duration > 0)) {
                in_media_info_->video_duration_ = in_format_ctx_->duration;
            }

            LOGD("<CAvDiagnoseEng::InStreamOpen> [VIDEO] idx=%d, codec=%d, fmt=%d, w=%d, h=%d, rotation=%d, fps=%d, duration=%" PRId64 ", start_time=%" PRId64 " \n",
                 in_media_info_->video_track_index_, in_media_info_->video_codec_, in_media_info_->color_format_,
                 in_media_info_->video_width_, in_media_info_->video_height_, in_media_info_->rotation_,
                 in_media_info_->frame_rate_, in_media_info_->video_duration_, in_video_stream_->start_time);

        } else if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            in_audio_stream_ = pAvStream;

            // 音频解码器
            in_audio_codec_ = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (in_audio_codec_ == nullptr)
            {
                LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] can not find audio codecId=%d\n",
                     pAvStream->codecpar->codec_id);
                continue;
            }


            // 解析音频流信息
            in_media_info_->audio_track_index_ = i;
            in_media_info_->audio_codec_ = static_cast<int32_t>(pAvStream->codecpar->codec_id);
            in_media_info_->audio_duration_ =
              static_cast<int64_t>(pAvStream->duration * 1000 * av_q2d(pAvStream->time_base) * 1000);
            in_media_info_->sample_foramt_ = static_cast<int32_t>(pAvStream->codecpar->format);
            in_media_info_->bytes_per_sample_  = av_get_bytes_per_sample((enum AVSampleFormat)(pAvStream->codecpar->format));
            in_media_info_->channels_ = pAvStream->codecpar->channels;
            in_media_info_->sample_rate_ = pAvStream->codecpar->sample_rate;
            if ((in_media_info_->audio_duration_ <= 0) && (in_format_ctx_->duration > 0)) {
                in_media_info_->audio_duration_ = in_format_ctx_->duration;
            }

            LOGD("<CAvDiagnoseEng::InStreamOpen> [AUDIO] idx=%d, codec=%d, fmt=%d, bytesPerSmpl=%d, channels=%d, samplerate=%d, duration=%" PRId64 ", start_time=%" PRId64 " \n",
                 in_media_info_->audio_track_index_, in_media_info_->audio_codec_,
                 in_media_info_->sample_foramt_, in_media_info_->bytes_per_sample_,
                 in_media_info_->channels_, in_media_info_->sample_rate_,
                 in_media_info_->audio_duration_, in_audio_stream_->start_time);
        }
    }
    if (in_media_info_->video_duration_ > in_media_info_->audio_duration_) {
        in_media_info_->file_duration_ = in_media_info_->video_duration_;
    } else {
        in_media_info_->file_duration_ = in_media_info_->audio_duration_;
    }
    in_media_info_->file_path_ = src_file_path_;



    //
    // 打开 视频解码器 和 视频格式转换器
    //
    if ((in_video_codec_ != nullptr) && (in_media_info_->video_track_index_ >= 0))
    {
        AVStream* pVideoStream = in_format_ctx_->streams[in_media_info_->video_track_index_];

        video_codec_ctx_ = AVCodecContextPtrCreate(in_video_codec_);
        if (video_codec_ctx_ == nullptr)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] fail to video AVCodecContextPtrCreate()\n");
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_parameters_to_context(video_codec_ctx_.get(), pVideoStream->codecpar);

        ret = avcodec_open2(video_codec_ctx_.get(), nullptr, nullptr);
        if (ret < 0)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] video fail to avcodec_open2(), ret=%d\n", ret);
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_flush_buffers(video_codec_ctx_.get());


        // 视频帧格式转换器，源视频帧和目标视频帧保持一样大小，固定输出NV12格式
        video_sws_ctx_ = SwsContextPtr(sws_getContext(
          pVideoStream->codecpar->width, pVideoStream->codecpar->height,
          static_cast<AVPixelFormat>(pVideoStream->codecpar->format),
          pVideoStream->codecpar->width, pVideoStream->codecpar->height, OUT_FRAME_FORAMT,
          SWS_BICUBIC, nullptr, nullptr, nullptr));
        if (video_sws_ctx_ == nullptr)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] fail to video sws_getContext()\n");
            Close();
            return XERR_CODEC_OPEN;
        }

        LOGD("<CAvDiagnoseEng::InStreamOpen> Open video decoder and converter successful!");
    }

    //
    // 打开 音频解码器 和 音频格式转换器
    //
    if ((in_audio_codec_ != nullptr) && (in_media_info_->audio_track_index_ >= 0)) {
        AVStream* pAudioStream = in_format_ctx_->streams[in_media_info_->audio_track_index_];

        audio_codec_ctx_ = AVCodecContextPtrCreate(in_audio_codec_);
        if (audio_codec_ctx_ == nullptr)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] fail to audio AVCodecContextPtrCreate()\n");
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_parameters_to_context(audio_codec_ctx_.get(), pAudioStream->codecpar);

        ret = avcodec_open2(audio_codec_ctx_.get(), nullptr, nullptr);
        if (ret < 0)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] audio fail to avcodec_open2(), ret=%d\n", ret);
            Close();
            return XERR_CODEC_OPEN;
        }
        avcodec_flush_buffers(audio_codec_ctx_.get());


        //
        // 创建 音频格式转换器
        //
        int64_t in_channel_layout = av_get_default_channel_layout(audio_codec_ctx_->channels);
        int     out_chnl_layout   = (OUT_CHANNELS == 1) ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
        AVSampleFormat out_smpl_fmt= (enum AVSampleFormat)(OUT_SAMPLE_FMT);
        int64_t dst_nb_samples    = av_rescale_rnd(audio_codec_ctx_->frame_size, OUT_SAMPLE_RATE,
                                                   audio_codec_ctx_->sample_rate, AV_ROUND_UP);
        audio_sws_ctx_  = SwrContextPtrCreate();
        swr_alloc_set_opts( audio_sws_ctx_.get(),
                            out_chnl_layout, out_smpl_fmt, OUT_SAMPLE_RATE,
                            in_channel_layout, audio_codec_ctx_->sample_fmt, audio_codec_ctx_->sample_rate,
                            0, nullptr);
        ret = swr_init(audio_sws_ctx_.get());
        if (ret < 0)
        {
            LOGE("<CAvDiagnoseEng::InStreamOpen> [ERROR] fail to audio swr_init()\n");
            Close();
            return XERR_CODEC_OPEN;
        }

        // 分配解码后的音频帧缓冲区
        audio_frame_ = MakeUniquePtr<AvAudioFrame>();
        audio_frame_->sample_fmt_ = out_smpl_fmt;
        audio_frame_->bytes_per_sample_ = av_get_bytes_per_sample(out_smpl_fmt);
        audio_frame_->channels_ = OUT_CHANNELS;
        audio_frame_->sample_rate_ = OUT_SAMPLE_RATE;
        audio_frame_->frame_index_ = 0;
        int32_t max_smpl_size = audio_frame_->bytes_per_sample_ * audio_frame_->channels_ * audio_frame_->sample_rate_;
        audio_frame_->sample_data_ = MakeUniquePtr<uint8_t[]>(max_smpl_size);;
        audio_frame_->frame_valid_ = false;

        LOGD("<CAvDiagnoseEng::InStreamOpen> Open audio decoder and converter successful!");
  }

  video_progress_.ResetAll();
  audio_progress_.ResetAll();

  input_eos_ = false;
  video_dec_eos_ = false;
  audio_dec_eos_ = false;

  LOGD("<CAvDiagnoseEng::InStreamOpen> done, srcFile=%s\n", src_file_path_.c_str());
    return XOK;
}

void CAvDiagnoseEng::InStreamClose() {

    if (video_codec_ctx_ != nullptr) {
        video_codec_ctx_.reset();
    }

    if (video_sws_ctx_ != nullptr) {
        video_sws_ctx_.reset();
    }

    if (audio_codec_ctx_ != nullptr) {
        audio_codec_ctx_.reset();
    }

    if (audio_sws_ctx_ != nullptr) {
        audio_sws_ctx_.reset();
    }

    if (video_frame_ != nullptr) {
        video_frame_->frame_data_.reset();
        video_frame_.reset();
    }

    if (audio_frame_ != nullptr) {
        audio_frame_->sample_data_.reset();
        audio_frame_.reset();
    }

    in_video_stream_ = NULL;
    in_audio_stream_ = NULL;
    in_video_codec_ = NULL;
    in_audio_codec_ = NULL;
    if (in_format_ctx_ != nullptr) {
        in_format_ctx_.reset();
        LOGD("<CAvDiagnoseEng::InStreamClose> done\n");
    }
}





///////////////////////////////////////////////////////////////////////
////////////////////////// Internal Methods ///////////////////////////
///////////////////////////////////////////////////////////////////////
int32_t CAvDiagnoseEng::ParseRotateAngle(AVStream *pAvStream)
{
    AVDictionaryEntry* dict_rotate = av_dict_get(pAvStream->metadata, "rotate", NULL, 0);
    double_t theta = (dict_rotate == NULL) ? 0 : atoi(dict_rotate->value);

    uint8_t* display_mtx = av_stream_get_side_data(pAvStream,AV_PKT_DATA_DISPLAYMATRIX, NULL);
    if (display_mtx != NULL) {
        theta = -av_display_rotation_get((int32_t *)display_mtx);
    }

    theta -= 360*floor(theta/360 + 0.9/360);
    if (fabs(theta - 90*round(theta/90)) > 2) {
    }

    int32_t rotation = 0;
    if (fabs(theta - 90) < 1.0)
    {
        rotation = 90;
    }

    else if (fabs(theta - 180) < 1.0 || fabs(theta + 180) < 1.0)
    {
        rotation = 180;
    }
    else if(fabs(theta - 270) < 1.0 || fabs(theta + 90) < 1.0)
    {
        rotation = 270;
    }
    else
    {
    }

    LOGD("<CAvDiagnoseEng::ParseRotateAngle> rotation=%d", rotation);
    return rotation;
}
