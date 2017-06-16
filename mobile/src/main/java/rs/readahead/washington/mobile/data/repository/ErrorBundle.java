package rs.readahead.washington.mobile.data.repository;

import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.HttpException;
import rs.readahead.washington.mobile.data.rest.BaseApi;
import rs.readahead.washington.mobile.data.rest.ErrorResponse;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import timber.log.Timber;


class ErrorBundle extends Throwable implements IErrorBundle {
    private int code = 0;
    private String message = null;


    ErrorBundle(final Throwable throwable) {
        super(throwable);

        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            retrofit2.Response response = null;


            try {
                response = httpException.response();

                if (response != null) {
                    Converter<ResponseBody, ErrorResponse> converter = BaseApi.getBaseRetrofit()
                            .responseBodyConverter(ErrorResponse.class, new Annotation[0]);

                    ErrorResponse errorResponse = converter.convert(response.errorBody());

                    if (errorResponse != null) {
                        code = errorResponse.getError().getCode();
                        message = errorResponse.getError().getMessage();
                    }
                }
            } catch (Throwable e) {
                try {
                    Timber.e(e, "Getting ErrorResponse from Throwable");
                } catch (Throwable ignored) {}
            } finally {
                try {
                    if (code == 0) {
                        if (response != null) {
                            code = response.code();
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public Throwable getExeption() {
        return getCause();
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
