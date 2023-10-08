#ifndef __BASE_FFMPEG_H__
#define __BASE_FFMPEG_H__

extern "C"
{
#include <libavcodec/avcodec.h>
#include <libavcodec/avfft.h>
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include <libavutil/audio_fifo.h>
#include <libavutil/avassert.h>
#include <libavutil/avstring.h>
#include <libavutil/channel_layout.h>
#include <libavutil/error.h>
#include <libavutil/frame.h>
#include <libavutil/imgutils.h>
#include <libavutil/log.h>
#include <libavutil/mathematics.h>
#include <libavutil/opt.h>
#include <libavutil/pixdesc.h>
#include <libavutil/samplefmt.h>
#include <libavutil/timestamp.h>
#include <libswscale/swscale.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libswresample/swresample.h>
#include <libavutil/hwcontext.h>
#include <libavutil/display.h>

}


#include <array>
#include <atomic>
#include <map>
#include <mutex>
#include <thread>
#include <unordered_map>
#include "BaseCommDefine.hpp"


template<typename T>
void NoFree(T *p)
{
}

template<typename T>
static inline void av_free_ex(T **t)
{
  av_freep(static_cast<void *>(t));
}


static inline void avcodec_free_context_ex(AVCodecContext **avctx)
{
  AVCodecContext *ctxPtr = *avctx;
  if (ctxPtr->hw_device_ctx)
  {
    av_buffer_unref(&ctxPtr->hw_device_ctx);
    ctxPtr->hw_device_ctx = nullptr;
  }
  avcodec_free_context(avctx);
}


template<typename T = void>
using AVFreePtr = std::unique_ptr<T, PointerDel<av_free_ex<T>>>;
template<typename T = void>
using AVNoFreePtr = std::unique_ptr<T, PointerDel<NoFree<T>>>;

// 无需释放, 资源全部是AVStream 所持有的codec 释放
using AVStreamPtr = AVNoFreePtr<AVStream>;

using AVFramePtr = std::unique_ptr<AVFrame, PointerDel<av_frame_free>>;
static inline auto AVFramePtrCreate()
{
  return AVFramePtr(av_frame_alloc());
}

using AVPacketPtr = std::unique_ptr<AVPacket, PointerDel<av_packet_free>>;
static inline auto AVPacketPtrCreate()
{
  return AVPacketPtr(av_packet_alloc());
}

using SwsContextPtr = std::unique_ptr<SwsContext, PointerDel<sws_freeContext>>;

using SwrContextPtr = std::unique_ptr<SwrContext, PointerDel<swr_free>>;
static inline auto SwrContextPtrCreate()
{
  return SwrContextPtr(swr_alloc());
}

using AVFormatAllocContextPtr = std::unique_ptr<AVFormatContext, PointerDel<avformat_free_context>>;
static inline auto AVFormatAllocContextPtrCreate(const char *filename,
                                                 const AVOutputFormat *oformat     = nullptr,
                                                 const char *               format_name = nullptr)
{
  AVFormatContext *ctx = nullptr;
  avformat_alloc_output_context2(&ctx, (AVOutputFormat*)oformat, format_name, filename);
  return AVFormatAllocContextPtr(ctx);
}

using AVFormatOpenContextPtr = std::unique_ptr<AVFormatContext, PointerDel<avformat_close_input>>;

static inline AVFormatOpenContextPtr AVFormatOpenContextPtrCreate(const char *url,
                                                const AVInputFormat *fmt     = nullptr,
                                                AVDictionary **           options = nullptr)
{
  AVFormatContext *ifmtCtx = nullptr;
  int res = avformat_open_input(&ifmtCtx, url, (AVInputFormat*)fmt, options);
  if (res < 0)
  {
    char* err_str = (char*)av_err2str(res);
    LOGE("AVFormatOpenContextPtrCreate failed %s: %s\n", url, err_str);
    return AVFormatOpenContextPtr();
  }
  return AVFormatOpenContextPtr(ifmtCtx);
}

using AVFormatAllocContextPtr = std::unique_ptr<AVFormatContext, PointerDel<avformat_free_context>>;

static inline AVFormatAllocContextPtr AVFormatAllocOutContextPtrCreate(
  const char *filename,
  AVOutputFormat *oformat = nullptr,
  const char * format_name = nullptr  )
{
  AVFormatContext *ctx = nullptr;
  int ret = avformat_alloc_output_context2(&ctx, oformat, format_name, filename);
  if (ret < 0)
  {
    char* err_str = (char*)av_err2str(ret);
    LOGE("AVFormatOutContextPtrCreate failed %s: %s\n", filename, err_str);
    return AVFormatAllocContextPtr();
  }

  return AVFormatAllocContextPtr(ctx);
}




static inline AVStreamPtr AVFormatNewStreamCreate(AVFormatContext *ctx, const AVCodec *codec)
{
  AVStream* stream = avformat_new_stream(ctx, codec);
  if (!stream)
  {
    LOGE("avformat_new_stream failed");
  }
  return AVStreamPtr(stream);
}


using AVCodecContextPtr = std::unique_ptr<AVCodecContext, PointerDel<avcodec_free_context_ex>>;
static inline auto AVCodecContextPtrCreate(const AVCodec *codec)
{
  return AVCodecContextPtr(avcodec_alloc_context3(codec));
}

// ExPtr表示入参需要是一个二级指针, 这样才能在析构的时候把源指针置空, 避免其他问题
using AVIOContextExPtr = std::unique_ptr<AVIOContext *, PointerDel<avio_closep>>;
static inline AVIOContextExPtr AVFormatAvIoOpen(AVFormatContext *ctx, const char *fileName, int flags = AVIO_FLAG_WRITE)
{
  auto ret   = avio_open(&ctx->pb, fileName, flags);
  auto ioCtx = AVIOContextExPtr(&ctx->pb);
  if (ret < 0)
  {
    LOGE("Could not avio_open output file '%s'", fileName);
    return nullptr;
  }
  return ioCtx;
}


using AVDictionaryPtr = std::unique_ptr<AVDictionary, PointerDel<av_dict_free>>;
using AVAudioFifoPtr  = std::unique_ptr<AVAudioFifo, PointerDel<av_audio_fifo_free>>;

using AVBufferRefPtr   = std::unique_ptr<AVBufferRef, PointerDel<av_buffer_unref>>;

#endif // __BASE_FFMPEG_H__
