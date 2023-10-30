/**
 * @file CAvDiagnoseEng.hpp
 * @brief This file define the convert engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-09-26
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#ifndef __AV_DIAGNOSE_ENG_H__
#define __AV_DIAGNOSE_ENG_H__

#include "comtypedef.hpp"
#include "AvParseProgress.hpp"


class CAvDiagnoseEng final
{
public:
  CAvDiagnoseEng();
    virtual ~CAvDiagnoseEng();

  int32_t Open(std::string src_file_path);
  int32_t Close();
  const AvMediaInfo* GetMediaInfoPtr();

  // 分析处理过程，需要循环的调用
  // 正常返回0或者正值，返回其他值表示失败或结束
  //   XERR_FILE_EOF: 转码完成；   XERR_FILE_READ: 数据包读取失败
  int32_t DoPrasing();

  // 获取当前分析进度，返回分析进度百分比
  int32_t GetParseProgress();


protected:
  int32_t InStreamOpen();
  void InStreamClose();
  int32_t ParseRotateAngle(AVStream *pAvStream);

  /*
   * @brief 从数据流中读取并送入一个数据包到音视频解码器
   * @param pkt_type : 表明送入的数据包类型； 0--没有送如包；  1--送入视频包； 2--送入音频包
   * @return 错误值
   */
  int32_t InputPacket(int32_t& pkt_type);

  /*
 * @brief 从解码器中解码一帧视频帧
 * @param 无
 * @return 返回的错误码, XERR_CODEC_INDATA 可以继续送入数据；
 *                    XERR_CODEC_DEC_EOS 视频流解码完成；
 *                    XERR_CODEC_DECODING 解码出现问题，不能再继续
 */
  int32_t DecodeVideoFrame();

  /*
 * @brief 从解码器中解码一帧音频帧
 * @param 无
 * @return 返回的错误码, XERR_CODEC_INDATA 可以继续送入数据；
 *                    XERR_CODEC_DEC_EOS 视频流解码完成；
 *                    XERR_CODEC_DECODING 解码出现问题，不能再继续
 */
  int32_t DecodeAudioFrame();


private:
  std::string             src_file_path_;


  AvMediaInfoPtr          in_media_info_ = nullptr;       ///< 输入媒体文件信息
  AVFormatOpenContextPtr  in_format_ctx_  = nullptr;      ///< 输入媒体格式上下文
  AVStream*               in_video_stream_ = NULL;        ///< 输入视频流，不需要释放，属于in_format_ctx_
  AVStream*               in_audio_stream_ = NULL;        ///< 输入音频流，不需要释放，属于in_format_ctx_
  AVCodecContextPtr       video_codec_ctx_ = nullptr;
  SwsContextPtr           video_sws_ctx_ = nullptr;
  AVCodecContextPtr       audio_codec_ctx_ = nullptr;
  SwrContextPtr           audio_sws_ctx_  = nullptr;
  AVCodec*                in_video_codec_ = NULL;         ///< 输入视频格式，不需要释放，属于in_format_ctx_
  AVCodec*                in_audio_codec_ = NULL;         ///< 输入视频格式，不需要释放，属于in_format_ctx_

  CAvParseProgress       video_progress_;
  CAvParseProgress       audio_progress_;
  AvVideoFramePtr        video_frame_ = nullptr;        ///< 当前解码出来的视频帧
  UniquePtr<uint8_t[]>   yuv_buffer_= nullptr;          ///< 解码后的YUV缓冲区
  AvAudioFramePtr        audio_frame_ = nullptr;        ///< 当前解码出来的音频帧

  int64_t                 parse_time_ = 0;                ///< 当前分析的时间
  int64_t                 parse_progress_ = 0;            ///< 分析进度百分比，范围 [0, 100]

  bool                    input_eos_ = false;            ///< 数据包送入是否已经完成
  bool                    video_dec_eos_ = false;        ///< 视频帧是否已经全部解码完成
  bool                    audio_dec_eos_ = false;        ///< 视频帧是否已经全部解码完成

};

#endif // __AV_DIAGNOSE_ENG_H__
