package cn.jack.okhttpdemo.bean;

/**
 * Created by cyg on 2019-08-28.
 */
public class UserInfo {


    /**
     * code : 200
     * msg : 请求成功
     * data : {"userId":1,"nickName":"24K纯帅","sex":1,"phone":"17764576259"}
     */

    private int code;
    private String msg;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * userId : 1
         * nickName : 24K纯帅
         * sex : 1
         * phone : 17764576259
         */

        private int userId;
        private String nickName;
        private int sex;
        private String phone;

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}