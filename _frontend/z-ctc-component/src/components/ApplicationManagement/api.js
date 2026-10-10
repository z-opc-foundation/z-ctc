// 重新导出 metaApp + ctcAuthorization API (供 ApplicationManagement 使用)
export {metaAppApi, configureApiBaseURL as configureMetaAppBaseURL} from '../_api/metaApp'
export {ctcAuthorizationApi, configureApiBaseURL as configureAuthzBaseURL} from '../_api/ctcAuthorization'
