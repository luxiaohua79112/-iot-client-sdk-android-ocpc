/**
 * @file CAvConvertEng.cpp
 * @brief This file implement the convert engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-09-26
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#include "comtypedef.hpp"
#include "AvConvertEng.hpp"



//
// FFMPEG内部日志输出
//
static void FfmpegLogCallback(void *ptr, int level, const char *fmt, va_list vl)
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
CAvConvertEng::CAvConvertEng()
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

    av_log_set_callback(FfmpegLogCallback);
}

CAvConvertEng::~CAvConvertEng()
{
    avformat_network_deinit();
}





int32_t CAvConvertEng::Open(std::string src_file_path, std::string dst_file_path)
{
    int32_t ret;
    int32_t i;

    src_file_path_ = src_file_path;
    dst_file_path_ = dst_file_path;

    ret = InStreamOpen();
    if (ret != XOK) {
        return ret;
    }

    ret = OutStreamOpen();
    cvt_time_ = 0;
    cvt_progress_ = 0;

    LOGD("<CAvConvertEng::Open> done, ret=%d\n", ret);
    return ret;
}

int32_t CAvConvertEng::Close()
{
    InStreamClose();
    OutStreamClose();

    LOGD("<CAvConvertEng::Close> done");
    return XOK;
}

const AvMediaInfo* CAvConvertEng::GetMediaInfoPtr() {
    return (in_media_info_.get());
}

int32_t CAvConvertEng::DoConvert() {
  int ret;

  //
  // 读取一个数据包
  //
  AVPacketPtr packet = AVPacketPtrCreate();
  ret = av_read_frame(in_format_ctx_.get(), packet.get());
  if (ret == AVERROR_EOF) {   // 媒体文件读取结束
      int writeRet = av_write_trailer(out_format_ctx_.get());  // 写入文件尾部信息
      cvt_progress_ = 100;
      LOGD("<CAvConvertEng::DoConvert> [ERROR] av_read_frame() is EOF, writeRet=%d, cvt_progress_=%d\n",
           writeRet, cvt_progress_);
      return XERR_FILE_EOF;

  } else if (ret < 0) {   // 数据包读取失败
      LOGE("<CAvConvertEng::DoConvert> [ERROR] fail to av_read_frame(), ret=%d\n", ret);
      return XERR_FILE_READ;
  }


  if (packet->stream_index == in_media_info_->video_track_index_) {    // 视频
      // 包的流索引改为输出视频流索引值
      packet->stream_index = out_video_stream_->index;

      // 时间戳做一个转换
      int64_t video_time = static_cast<int64_t>(packet->pts * 1000 * av_q2d(in_video_stream_->time_base) * 1000);
      int64_t in_video_pts = packet->pts - in_video_stream_->start_time;
      int64_t out_video_pts = av_rescale_q(in_video_pts,in_video_stream_->time_base, out_video_stream_->time_base);
      int64_t in_video_dts = packet->dts - in_video_stream_->start_time;
      int64_t out_video_dts = av_rescale_q(in_video_dts,in_video_stream_->time_base, out_video_stream_->time_base);
      packet->pts = out_video_pts;
      packet->dts = out_video_dts;

      float video_time_sec = (float)(video_time / 1000000.0f);
      LOGD("<CAvConvertEng::DoConvert> [VIDEO] video_time_sec=%.3f, video_time=%" PRId64 ", pkt_pts=%" PRId64
            ", in_video_pts=%" PRId64 ", out_video_pts=%" PRId64 ", in_video_dts=%" PRId64 ", out_video_dts=%" PRId64 " \n",
           video_time_sec, video_time, packet->pts, in_video_pts, out_video_pts, in_video_dts, out_video_dts);

      // 计算当前进度
      cvt_time_ = static_cast<int64_t>(out_video_pts * 1000 * av_q2d(out_video_stream_->time_base) * 1000);
      cvt_progress_ = (int32_t)(cvt_time_ * 100L / in_media_info_->video_duration_);
      if (cvt_progress_ > 100) {
        cvt_progress_ = 100;
      }

  } else if (packet->stream_index == in_media_info_->audio_track_index_) { // 音频
      // 包的流索引改为输出音频流索引值
      packet->stream_index = out_audio_stream_->index;

      // 时间戳做一个转换
      int64_t audio_time = static_cast<int64_t>(packet->pts * 1000 * av_q2d(in_audio_stream_->time_base) * 1000);
      int64_t in_audio_pts = packet->pts - in_audio_stream_->start_time;
      int64_t out_audio_pts = av_rescale_q(in_audio_pts,in_audio_stream_->time_base, out_audio_stream_->time_base);
      int64_t in_audio_dts = packet->dts - in_audio_stream_->start_time;
      int64_t out_audio_dts = av_rescale_q(in_audio_dts,in_audio_stream_->time_base, out_audio_stream_->time_base);
      packet->pts = out_audio_pts;
      packet->dts = out_audio_dts;
      float audio_time_sec = (float)(audio_time / 1000000.0f);
      LOGD("<CAvConvertEng::DoConvert> [AUDIO] audio_time_sec=%.3f, audio_time=%" PRId64 ", pkt_pts=%" PRId64
            ", in_audio_pts=%" PRId64 ", out_audio_pts=%" PRId64 ", in_audio_dts=%" PRId64 ", out_audio_dts=%" PRId64 " \n",
           audio_time_sec, audio_time, packet->pts, in_audio_pts, out_audio_pts, in_audio_dts, out_audio_dts);

      // 如果当前没有视频流，则使用音频流计算当前进度
      if (in_video_codec_ == NULL) {
        cvt_time_ =
          static_cast<int64_t>(out_audio_pts * 1000 * av_q2d(out_audio_stream_->time_base) * 1000);
        cvt_progress_ = (int32_t) (cvt_time_ * 100L / in_media_info_->audio_duration_);
        if (cvt_progress_ > 100) {
          cvt_progress_ = 100;
        }
      }

  }


  ret = av_interleaved_write_frame(out_format_ctx_.get(), packet.get());
  if (ret < 0) {   // 写入数据包失败
      LOGE("<CAvConvertEng::DoConvert> [ERROR] fail to av_interleaved_write_frame(), ret=%d\n", ret);
      return XERR_FILE_WRITE;
  }

  LOGD("<CAvConvertEng::DoConvert> done, ret=%d, cvt_progress_=%d\n", ret, cvt_progress_);
  return XOK;
}

int32_t CAvConvertEng::GetCvtProgress() {
    return cvt_progress_;
}



///////////////////////////////////////////////////////////////////////////////
/////////////////////// Internal Methods for Input Stream /////////////////////
///////////////////////////////////////////////////////////////////////////////
int32_t CAvConvertEng::InStreamOpen() {
    int ret, i;

    in_media_info_ = MakeUniquePtr<AvMediaInfo>();

    // 打开媒体文件
    in_format_ctx_ = AVFormatOpenContextPtrCreate(src_file_path_.c_str());
    if (in_format_ctx_ == nullptr)  {
        LOGE("<CAvConvertEng::InStreamOpen> [ERROR] format_ctx_ is NULL\n");
        return XERR_FILE_OPEN;
    }

    // 查询所有的媒体流
    ret = avformat_find_stream_info(in_format_ctx_.get(), nullptr);
    if (ret < 0)   {
        LOGE("<CAvConvertEng::InStreamOpen> [ERROR] fail to avformat_find_stream_info(), ret=%d", ret);
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
                LOGE("<CAvConvertEng::InStreamOpen> [ERROR] invalid resolution, streamIndex=%d\n", i);
                continue;
            }
            in_video_stream_ = pAvStream;

            // 视频解码器
            in_video_codec_ = avcodec_find_decoder(pAvStream->codecpar->codec_id);
            if (in_video_codec_ == nullptr)
            {
                LOGE("<CAvConvertEng::Open> [ERROR] can not find video codecId=%d\n",
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

            LOGD("<CAvConvertEng::InStreamOpen> [VIDEO] idx=%d, codec=%d, fmt=%d, w=%d, h=%d, rotation=%d, fps=%d, duration=%" PRId64 ", start_time=%" PRId64 " \n",
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
                LOGE("<CAvConvertEng::InStreamOpen> [ERROR] can not find audio codecId=%d\n",
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

            LOGD("<CAvConvertEng::InStreamOpen> [AUDIO] idx=%d, codec=%d, fmt=%d, bytesPerSmpl=%d, channels=%d, samplerate=%d, duration=%" PRId64 ", start_time=%" PRId64 " \n",
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

    LOGD("<CAvConvertEng::InStreamOpen> done, srcFile=%s\n", src_file_path_.c_str());
    return XOK;
}

void CAvConvertEng::InStreamClose() {
    in_video_stream_ = NULL;
    in_audio_stream_ = NULL;
    in_video_codec_ = NULL;
    in_audio_codec_ = NULL;
    if (in_format_ctx_ != nullptr) {
        in_format_ctx_.reset();
        LOGD("<CAvConvertEng::InStreamClose> done\n");
    }
}




///////////////////////////////////////////////////////////////////////////////
/////////////////////// Internal Methods for Output Stream /////////////////////
///////////////////////////////////////////////////////////////////////////////
int32_t CAvConvertEng::OutStreamOpen() {
    bool bGlobalHeader = false;
    int ret;

    // 创建输出流格式上下文
    out_format_ctx_ = AVFormatAllocOutContextPtrCreate(dst_file_path_.c_str());
    if (out_format_ctx_ == nullptr)
    {
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to AVFormatAllocOutContextPtrCreate()\n");
        return XERR_FILE_OPEN;
    }
    if (out_format_ctx_->oformat->flags & AVFMT_GLOBALHEADER)
    {
        LOGD("<CAvConvertEng::OutStreamOpen> AVFMT_GLOBALHEADER\n");
        bGlobalHeader = true;
    }


    // 创建写入的视频流
    if (in_video_codec_ != NULL) {
      out_video_stream_ = AVFormatNewStreamCreate(out_format_ctx_.get(), in_video_codec_);
      if (out_video_stream_ == nullptr) {
        out_format_ctx_.reset();
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to create video stream\n");
        return XERR_FILE_OPEN;
      }
      ret = avcodec_parameters_copy(out_video_stream_->codecpar, in_video_stream_->codecpar);
      if (ret < 0) {
        out_format_ctx_.reset();
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to avcodec_parameters_copy(), ret=%d\n",
             ret);
        return XERR_FILE_OPEN;
      }
      out_video_stream_->time_base = in_video_stream_->time_base;
      out_video_stream_->codecpar->codec_tag =
        0; // MKTAG('h', 'v', 'c', '1');  // 默认是 hev1 的tag, 需要修改
      out_video_stream_->start_time = 0;          // 时间戳总是从0开始
    }

    // 创建写入的音频流
    if (in_audio_codec_ != NULL) {
      out_audio_stream_ = AVFormatNewStreamCreate(out_format_ctx_.get(), in_audio_codec_);
      if (out_audio_stream_ == nullptr) {
        out_format_ctx_.reset();
        out_video_stream_.reset();
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to create audio stream\n");
        return XERR_FILE_OPEN;
      }
      ret = avcodec_parameters_copy(out_audio_stream_->codecpar, in_audio_stream_->codecpar);
      if (ret < 0) {
        out_format_ctx_.reset();
        out_video_stream_.reset();
        out_audio_stream_.reset();
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to avcodec_parameters_copy(), ret=%d\n",
             ret);
        return XERR_FILE_OPEN;
      }
      out_audio_stream_->time_base = in_audio_stream_->time_base;
      out_audio_stream_->codecpar->codec_tag = 0;  // 设置0 自动选择codec_tag，不能用输入流的
      out_audio_stream_->start_time = 0;          // 时间戳总是从0开始
    }

    //  创建输出生成文件上下文
    out_io_ctx_ = AVFormatAvIoOpen(out_format_ctx_.get(), dst_file_path_.c_str());
    if (out_io_ctx_ == nullptr)
    {
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to avio_open()\n");
        out_format_ctx_.reset();
        out_video_stream_.reset();
        out_audio_stream_.reset();
        return XERR_FILE_OPEN;
    }

    AVStream* out_video_nativeptr = out_video_stream_.get();
    AVStream* out_audio_nativeptr = out_audio_stream_.get();

    //
    // 写入文件头信息
    //
    ret = avformat_write_header(out_format_ctx_.get(), nullptr);
    if (ret < 0)
    {
        if (ret == AVERROR_INVALIDDATA) {
            LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] AVERROR_INVALIDDATA\n");
        }
        char* err_str = (char*)av_err2str(ret);
        LOGE("<CAvConvertEng::OutStreamOpen> [ERROR] fail to avformat_write_header(), ret=%d, %s\n",
             ret, err_str);
        out_format_ctx_.reset();
        out_video_stream_.reset();
        out_audio_stream_.reset();
        return XERR_FILE_OPEN;
    }


    LOGD("<CAvConvertEng::OutStreamOpen> done, dstFile=%s\n", dst_file_path_.c_str());
    return XOK;
}


void CAvConvertEng::OutStreamClose() {
    if (out_format_ctx_ != nullptr) {
        out_format_ctx_.reset();
    }

    if (out_io_ctx_ != nullptr)   {
        out_io_ctx_.reset();
        LOGD("<CAvConvertEng::OutStreamClose> done\n");
    }
}


///////////////////////////////////////////////////////////////////////
////////////////////////// Internal Methods ///////////////////////////
///////////////////////////////////////////////////////////////////////
int32_t CAvConvertEng::ParseRotateAngle(AVStream *pAvStream)
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

    LOGD("<CAvConvertEng::ParseRotateAngle> rotation=%d", rotation);
    return rotation;
}
