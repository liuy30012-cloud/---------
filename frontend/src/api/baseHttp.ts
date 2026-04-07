import axios from 'axios'
import { API_CONFIG } from '../config'

export const baseHttp = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
})

export default baseHttp
