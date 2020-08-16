import os

import pandas as pd

import constants
import files


def _fmt_timedelta(seconds):
    hours, remainder = divmod(seconds, 3600)
    minutes, _ = divmod(remainder, 60)
    return ('{:02d}:{:02d}').format(int(hours), int(minutes)) 

def _get_filterd_columns_list(df):
    cols = ['4 pax', '3 pax', '2 pax', '1 pax', '0 pax', 'stay']
    return [c for c in cols if c in df.columns]

def _get_occupancy_stats_drt(alg_path):
    """ reads the DRT occupancy data and replace the time column with timedelta"""
    fname = "drt_occupancy_time_profiles_av.txt"
    full_path = files.get_last_iter_file(alg_path, fname)
    df = pd.read_table(full_path)
    df["time"] = df.index * 5 * 60
    return df


def _get_occupancy_stats_amod(alg_path):
    """ reads amod occupancy stats add column names and a time columns"""
    full_path = os.path.join(alg_path, "output", "data", "statusDistributionNumPassengers")
    columns = ['4 pax', '3 pax', '2 pax', '1 pax', '0 pax', 'rebalance', 'stay', 'off-service']
    df = files.read_amod_csv(full_path)
    col_num = len(df.columns)
    # choose column names according to the number of columns in the data
    df.columns = columns[-col_num:]
    df["0 pax"] += df["rebalance"]
    df["time"] = df.index * 10
    return df


def _percent_in_window(df, start_time, end_time, time_column="time"):
    """ returns the percent of each column in the window """
    timedelta = pd.to_timedelta(df[time_column], unit="s")
    times = ((timedelta >= pd.to_timedelta(start_time))
             & (timedelta <= pd.to_timedelta(end_time)))
    cols = _get_filterd_columns_list(df)
    means = df[times][cols].mean(axis=0, numeric_only=True)
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


def get_occupancy_per_hour(alg_path):
    """ reads the occupanct info and return a stacked graph of the data"""
    alg = os.path.basename(alg_path)
    if files.is_drt(alg_path):
        df = _get_occupancy_stats_drt(alg_path)
    else:
        df = _get_occupancy_stats_amod(alg_path)
    
    return df
    
def plot_occuancy_data(occ_data, alg, ax=None):
    
    cols = _get_filterd_columns_list(occ_data)
    ax = occ_data.plot.area(stacked=True,
                 color=[constants.get_color(col, "Set1") for col in cols],
                 x="time", y=cols, alpha=0.7,
                 ax=ax)
    
    ax.set_title(alg)
    
    ax.set_xlabel("Time of day [h]")
    ticks_num = 6
    ax.xaxis.set_ticks([tick * (30//ticks_num) * 3600 for tick in range(1,ticks_num)])
    ax.set_xticklabels([ _fmt_timedelta(x) for x in ax.get_xticks()])

    ax.set_ylabel("No. of vehicles")
    
    return ax
