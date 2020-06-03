import os

import pandas as pd

import files


def _get_wait_time_stats_amod(alg_path):
    full_path = os.path.join(alg_path, "output", "data", "RequestTravelTimes")
    df = files.read_amod_csv(full_path)
    df["wait time"] = df["pickup time"] - df["submission time"]
    return {"mean wait": df.mean(axis=0)["wait time"],
            "95p wait": df.quantile(.95)["wait time"], }


def _get_wait_time_stats_drt(alg_path):
    full_path = os.path.join(alg_path, "output", "drt_customer_stats_av.csv")
    df = pd.read_csv(full_path, sep=";")
    last_it = df.iloc[-1]
    return {"mean wait": last_it["wait_average"],
            "95p wait": last_it["wait_p95"], }


def get_wait_stats(alg_path):
    """ returns mean and 95p for that algorithm"""
    if files.is_drt(alg_path):
        result = _get_wait_time_stats_drt(alg_path)
    else:
        result = _get_wait_time_stats_amod(alg_path)
    return pd.Series(result)
