/**
 * @file CAvConvertEng.hpp
 * @brief This file define the convert engine
 * @author xiaohua.lu
 * @email 2489186909@qq.com    luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-09-26
 * @license Copyright (C) 2021 LuXiaoHua. All rights reserved.
 */
#ifndef __AV_CONVERT_ENG_H__
#define __AV_CONVERT_ENG_H__

#include "comtypedef.hpp"



class CAvConvertEng final
{
public:
  CAvConvertEng();
    virtual ~CAvConvertEng();

  int32_t Open(std::string src_file_path, std::string dst_file_path);
  int32_t Close();
  const AvMediaInfo* GetMediaInfoPtr();

  // 转换处理过程，需要循环的调用
  // 正常返回0或者正值，返回其他值表示失败或结束
  //   XERR_FILE_EOF: 转码完成；   XERR_FILE_READ: 数据包读取失败；  XERR_FILE_WRITE: 写入失败
  int32_t DoConvert();


protected:
  int32_t InStreamOpen();
  void InStreamClose();

  int32_t OutStreamOpen();
  void OutStreamClose();

  int32_t ParseRotateAngle(AVStream *pAvStream);

private:
  std::string             src_file_path_;
  std::string             dst_file_path_;

  AvMediaInfoPtr          in_media_info_ = nullptr;       ///< 输入媒体文件信息
  AVFormatOpenContextPtr  in_format_ctx_  = nullptr;      ///< 输入媒体格式上下文
  AVStream*               in_video_stream_ = NULL;        ///< 输入视频流，不需要释放，属于in_format_ctx_
  AVStream*               in_audio_stream_ = NULL;        ///< 输入音频流，不需要释放，属于in_format_ctx_
  AVCodec*                in_video_codec_ = NULL;         ///< 输入视频格式，不需要释放，属于in_format_ctx_
  AVCodec*                in_audio_codec_ = NULL;         ///< 输入视频格式，不需要释放，属于in_format_ctx_

  AVFormatAllocContextPtr out_format_ctx_ = nullptr;      ///< 输出媒体格式上下文
  AVIOContextExPtr        out_io_ctx_  = nullptr;         ///< 输出文件读写上下文
  AVStreamPtr             out_video_stream_ = nullptr;    ///< 输出视频流
  AVStreamPtr             out_audio_stream_ = nullptr;    ///< 输出音频流


};

#endif // __AV_CONVERT_ENG_H__
