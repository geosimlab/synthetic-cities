import os

import pandas as pd

import constants
import files


def _get_occupancy_stats_drt(alg_path):
    """ reads the DRT occupancy data and replace the time column with timedelta"""
    fname = "drt_occupancy_time_profiles_av.txt"
    full_path = files.get_last_iter_file(alg_path, fname)
    df = pd.read_table(full_path)
    df["time"] = pd.to_timedelta(df.index * 5, unit='minutes')
    return df


def _get_occupancy_stats_amod(alg_path):
    """ reads amod occupancy stats add column names and a time columns"""
    full_path = os.path.join(alg_path, "output", "data", "statusDistributionNumPassengers")
    columns = ['4 pax', '3 pax', '2 pax', '1 pax', '0 pax', 'rebalance', 'stay', 'off-service']
    df = files.read_amod_csv(full_path)
    col_num = len(df.columns)
    # choose column names according to the number of columns in the data
    df.columns = columns[-col_num:]
    df["time"] = pd.to_timedelta(df.index * 10, unit='seconds')
    return df


def _percent_in_window(df, start_time, end_time, time_column="time"):
    """ returns the percent of each column in the window """
    times = ((df[time_column] >= pd.to_timedelta(start_time))
             & (df[time_column] <= pd.to_timedelta(end_time)))
    means = df[times].mean(axis=0, numeric_only=True)
    return means / means.sum()


def get_ocucpancy_aggregation(alg_path):
    """ reads the occupanct info and return a stacked graph of the data"""
    alg = os.path.basename(alg_path)
    if files.is_drt(alg_path):
        df = _get_occupancy_stats_drt(alg_path)
    else:
        df = _get_occupancy_stats_amod(alg_path)

    morning_means = _percent_in_window(df, "6hr", "9hr")
    evening_means = _percent_in_window(df, "15hr", "18hr")
    return pd.concat([morning_means, evening_means], keys=["morning", "evening"])


def get_occupancy_graphs(alg_path):
    """ reads the occupanct info and return a stacked graph of the data"""
    cols = ['4 pax', '3 pax', '2 pax', '1 pax', '0 pax', 'rebalance', 'stay']
    alg = os.path.basename(alg_path)
    if files.is_drt(alg_path):
        df = _get_occupancy_stats_drt(alg_path)
    else:
        df = _get_occupancy_stats_amod(alg_path)

    cols = [c for c in cols if c in df.columns]
    ax = df.plot.area(stacked=True,
                      title=alg + " number of passengers",
                      color=[constants.COL_TO_COLOR.get(col) for col in cols],
                      x="time", y=cols, figsize=(12, 6))
    ax.title.set_size(18)
    return ax
