import 'axios'

declare module 'axios' {
  interface AxiosRequestConfig<D = any> {
    _captchaRetried?: boolean
    skipCaptchaChallenge?: boolean
  }

  interface InternalAxiosRequestConfig<D = any> {
    _captchaRetried?: boolean
    skipCaptchaChallenge?: boolean
  }
}
